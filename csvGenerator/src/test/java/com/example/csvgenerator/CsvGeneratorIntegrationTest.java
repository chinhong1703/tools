package com.example.csvgenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvGeneratorIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void writesCsvToTempDirectory() throws IOException {
        MetricsSink metricsSink = new NoopMetricsSink();
        StructuredLogger logger = new StructuredLogger();
        CsvGenerator generator = new CsvGenerator(metricsSink, logger);

        CsvGeneratorOptions options = new CsvGeneratorOptions(
                tempDir,
                false,
                0L,
                1024 * 1024,
                5,
                Clock.systemUTC()
        );

        List<UserRecord> users = List.of(
                new UserRecord(10, "Test User", "test@example.com")
        );

        CsvGenerationResult result = generator.generate(new CsvGenerationRequest<>(CsvFileType.USERS, users), options);

        Path outputFile = tempDir.resolve("users").resolve("users.csv");
        assertTrue(Files.exists(outputFile));
        assertTrue(result.outputFiles().contains(outputFile));

        String content = Files.readString(outputFile);
        assertTrue(content.contains("id,name,email"));
        assertTrue(content.contains("10,Test User,test@example.com"));
    }

    @Test
    void streamsLargeRowSet() throws IOException {
        MetricsSink metricsSink = new NoopMetricsSink();
        StructuredLogger logger = new StructuredLogger();
        CsvGenerator generator = new CsvGenerator(metricsSink, logger);

        CsvGeneratorOptions options = CsvGeneratorOptions.defaultOptions(tempDir);

        Iterable<OrderRecord> orders = () -> new Iterator<>() {
            private int current;
            private final int max = 10_000;

            @Override
            public boolean hasNext() {
                return current < max;
            }

            @Override
            public OrderRecord next() {
                current++;
                return new OrderRecord(current, current % 100, current * 1.5);
            }
        };

        CsvGenerationResult result = generator.generate(new CsvGenerationRequest<>(CsvFileType.ORDERS, orders), options);

        List<Path> outputFiles = new ArrayList<>(result.outputFiles());
        assertTrue(!outputFiles.isEmpty());
        assertTrue(Files.size(outputFiles.getFirst()) > 0);
    }
}
