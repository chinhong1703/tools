package com.example.csvgenerator;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CsvGenerator {
    private final MetricsSink metricsSink;
    private final StructuredLogger logger;

    public CsvGenerator(MetricsSink metricsSink, StructuredLogger logger) {
        this.metricsSink = Objects.requireNonNull(metricsSink, "metricsSink");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public <T> CsvGenerationResult generate(CsvGenerationRequest<T> request, CsvGeneratorOptions options) throws IOException {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(options, "options");

        CsvFileType fileType = request.fileType();
        OutputNamingConfig naming = fileType.namingConfig();
        Path outputDir = options.outputRoot().resolve(naming.directoryName());
        Files.createDirectories(outputDir);

        CsvFormatSettings formatSettings = fileType.formatSettings();
        CsvRowMapper<T> rowMapper = fileType.rowMapper();

        boolean splitting = options.enableSplitting() && options.splitSizeBytes() > 0;
        long splitThreshold = options.splitSizeBytes();

        List<Path> outputFiles = new ArrayList<>();
        Instant start = Instant.now();

        long totalRowsWritten = 0;
        long totalBytesWritten = 0;
        int partNumber = 1;
        int skipCount = 0;

        IteratorWithState<T> iterator = new IteratorWithState<>(request.rows().iterator());
        CsvPartWriter partWriter = null;
        try {
            while (iterator.hasNext() || partWriter == null) {
                if (partWriter == null) {
                    partWriter = openPartWriter(fileType, outputDir, naming, formatSettings, options, partNumber);
                    partWriter.csvWriter().writeHeader(fileType.headers());
                }

                if (!iterator.hasNext()) {
                    break;
                }

                T row = iterator.next();
                try {
                    String[] fields = rowMapper.mapRow(row);
                    partWriter.csvWriter().writeRow(fields);
                    totalRowsWritten++;
                } catch (Exception ex) {
                    skipCount++;
                    metricsSink.incrementCounter("csv.rows_skipped", 1, tags(fileType, partWriter.finalPath(), partNumber));
                    logger.warn("csv.row.skipped", logFields(fileType, partWriter.finalPath(), partNumber, skipCount, totalRowsWritten, partWriter.bytesWritten()));
                    if (skipCount > options.maxSkips()) {
                        throw new IOException("Exceeded max skipped rows: " + options.maxSkips(), ex);
                    }
                }

                if (splitting && partWriter.bytesWritten() >= splitThreshold && iterator.hasNext()) {
                    partWriter.closeAndFinalize();
                    outputFiles.add(partWriter.finalPath());
                    totalBytesWritten += partWriter.bytesWritten();
                    partWriter = null;
                    partNumber++;
                }
            }

            if (partWriter != null) {
                partWriter.closeAndFinalize();
                outputFiles.add(partWriter.finalPath());
                totalBytesWritten += partWriter.bytesWritten();
            }

            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            Map<String, String> tags = tags(fileType, outputDir, partNumber);
            metricsSink.incrementCounter("csv.rows_written", totalRowsWritten, tags);
            metricsSink.incrementCounter("csv.bytes_written", totalBytesWritten, tags);
            metricsSink.incrementCounter("csv.rows_skipped", skipCount, tags);
            metricsSink.recordTimer("csv.generation_ms", elapsedMs, tags);

            logger.info("csv.generation.complete", logFields(fileType, outputDir, partNumber, skipCount, totalRowsWritten, totalBytesWritten));

            return new CsvGenerationResult(fileType, totalRowsWritten, totalBytesWritten, outputFiles.size(), outputFiles);
        } catch (IOException ex) {
            if (partWriter != null) {
                partWriter.closeOnFailure();
            }
            throw ex;
        }
    }

    private CsvPartWriter openPartWriter(CsvFileType fileType,
                                        Path outputDir,
                                        OutputNamingConfig naming,
                                        CsvFormatSettings formatSettings,
                                        CsvGeneratorOptions options,
                                        int partNumber) throws IOException {
        String baseName = naming.buildBaseFileName(options.clock());
        String fileName = buildFileName(baseName, partNumber, options.enableSplitting());
        Path finalPath = outputDir.resolve(fileName);
        Path tempPath = outputDir.resolve(fileName + ".tmp");

        OutputStream fileStream = Files.newOutputStream(tempPath);
        CountingOutputStream countingStream = new CountingOutputStream(fileStream);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(countingStream, options.bufferSizeBytes());
        Writer writer = new BufferedWriter(new OutputStreamWriter(bufferedOutputStream, formatSettings.charset()), options.bufferSizeBytes());
        CsvWriter csvWriter = new CsvWriter(writer, formatSettings);

        logger.info("csv.part.opened", logFields(fileType, finalPath, partNumber, 0, 0, 0));
        return new CsvPartWriter(csvWriter, countingStream, tempPath, finalPath, fileType, partNumber, logger);
    }

    private String buildFileName(String baseName, int partNumber, boolean splitEnabled) {
        if (splitEnabled) {
            return baseName + "_part-" + String.format("%05d", partNumber) + ".csv";
        }
        return baseName + ".csv";
    }

    private Map<String, String> tags(CsvFileType fileType, Path outputPath, int partNumber) {
        Map<String, String> tags = new HashMap<>();
        tags.put("fileType", fileType.typeName());
        tags.put("outputPath", outputPath.toString());
        tags.put("partNumber", String.valueOf(partNumber));
        return tags;
    }

    private Map<String, Object> logFields(CsvFileType fileType, Path outputPath, int partNumber, int skipCount, long rowsWritten, long bytesWritten) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("fileType", fileType.typeName());
        fields.put("outputPath", outputPath.toString());
        fields.put("partNumber", partNumber);
        fields.put("rowsWritten", rowsWritten);
        fields.put("bytesWritten", bytesWritten);
        fields.put("skipped", skipCount);
        return fields;
    }

    private static final class CsvPartWriter {
        private final CsvWriter csvWriter;
        private final CountingOutputStream countingStream;
        private final Path tempPath;
        private final Path finalPath;
        private final CsvFileType fileType;
        private final int partNumber;
        private final StructuredLogger logger;

        private CsvPartWriter(CsvWriter csvWriter,
                              CountingOutputStream countingStream,
                              Path tempPath,
                              Path finalPath,
                              CsvFileType fileType,
                              int partNumber,
                              StructuredLogger logger) {
            this.csvWriter = csvWriter;
            this.countingStream = countingStream;
            this.tempPath = tempPath;
            this.finalPath = finalPath;
            this.fileType = fileType;
            this.partNumber = partNumber;
            this.logger = logger;
        }

        public CsvWriter csvWriter() {
            return csvWriter;
        }

        public long bytesWritten() {
            return countingStream.bytesWritten();
        }

        public Path finalPath() {
            return finalPath;
        }

        public void closeAndFinalize() throws IOException {
            csvWriter.close();
            try {
                Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
                logger.warn("csv.part.atomic_move_not_supported", Map.of(
                        "fileType", fileType.typeName(),
                        "outputPath", finalPath.toString(),
                        "partNumber", partNumber
                ));
            }
        }

        public void closeOnFailure() {
            try {
                csvWriter.close();
            } catch (IOException ignored) {
                // ignored
            }
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                // ignored
            }
        }
    }

    private static final class IteratorWithState<T> {
        private final java.util.Iterator<T> iterator;
        private boolean checked;
        private boolean hasNext;

        private IteratorWithState(java.util.Iterator<T> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            if (!checked) {
                hasNext = iterator.hasNext();
                checked = true;
            }
            return hasNext;
        }

        public T next() {
            if (!checked) {
                hasNext = iterator.hasNext();
            }
            checked = false;
            if (!hasNext) {
                throw new java.util.NoSuchElementException();
            }
            return iterator.next();
        }
    }
}
