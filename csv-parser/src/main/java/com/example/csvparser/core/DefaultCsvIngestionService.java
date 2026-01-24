package com.example.csvparser.core;

import com.example.csvparser.adapter.univocity.ParsedCsv;
import com.example.csvparser.adapter.univocity.UniVocityCsvReader;
import com.example.csvparser.core.conversion.ConverterRegistry;
import com.example.csvparser.core.idempotency.ChecksumService;
import com.example.csvparser.core.idempotency.ProcessedCsvFile;
import com.example.csvparser.core.idempotency.ProcessingRegistry;
import com.example.csvparser.core.idempotency.ProcessingStatus;
import com.example.csvparser.core.mapping.AnnotationIntrospector;
import com.example.csvparser.core.mapping.FieldMapping;
import com.example.csvparser.core.mapping.HeaderIndex;
import com.example.csvparser.core.mapping.MappingPlan;
import com.example.csvparser.core.mapping.RowMapper;
import com.example.csvparser.core.mapping.RowMappingResult;
import com.example.csvparser.core.validation.BeanValidatorAdapter;
import com.univocity.parsers.common.TextParsingException;
import jakarta.validation.ConstraintViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultCsvIngestionService implements CsvIngestionService {
    private final ProcessingRegistry processingRegistry;
    private final ChecksumService checksumService;
    private final AnnotationIntrospector annotationIntrospector;
    private final ConverterRegistry converterRegistry;
    private final UniVocityCsvReader csvReader;

    public DefaultCsvIngestionService(ProcessingRegistry processingRegistry) {
        this(processingRegistry,
                new ChecksumService(),
                new AnnotationIntrospector(),
                new ConverterRegistry(),
                new UniVocityCsvReader());
    }

    public DefaultCsvIngestionService(
            ProcessingRegistry processingRegistry,
            ChecksumService checksumService,
            AnnotationIntrospector annotationIntrospector,
            ConverterRegistry converterRegistry,
            UniVocityCsvReader csvReader
    ) {
        this.processingRegistry = processingRegistry;
        this.checksumService = checksumService;
        this.annotationIntrospector = annotationIntrospector;
        this.converterRegistry = converterRegistry;
        this.csvReader = csvReader;
    }

    @Override
    public <T> IngestionResult<T> ingest(CsvIngestionRequest request, Class<T> targetType) {
        long start = System.currentTimeMillis();
        CsvFormatOptions formatOptions = request.formatOptions() == null ? CsvFormatOptions.defaults() : request.formatOptions();
        CsvMappingOptions mappingOptions = request.mappingOptions() == null ? CsvMappingOptions.defaults() : request.mappingOptions();
        CsvValidationOptions validationOptions = request.validationOptions() == null
                ? CsvValidationOptions.defaults(isValidatorAvailable())
                : request.validationOptions();

        String checksum = checksumService.sha256(request.content());
        if (!request.forceReprocess()) {
            Optional<ProcessedCsvFile> processed = processingRegistry.findProcessed(request.objectKey(), checksum);
            if (processed.isPresent() && processed.get().getStatus() == ProcessingStatus.PROCESSED) {
                return new IngestionResult<>(
                        IngestionStatus.SKIPPED_ALREADY_PROCESSED,
                        request.objectKey(),
                        checksum,
                        List.of(),
                        List.of(),
                        List.of(),
                        new ParseStats(0, 0, 0, System.currentTimeMillis() - start)
                );
            }
        }

        List<RowError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<T> records = new ArrayList<>();
        ParsedCsv parsedCsv;
        try {
            parsedCsv = csvReader.parse(request.content(), formatOptions);
        } catch (TextParsingException exception) {
            errors.add(new RowError(1, null, null, exception.getMessage(), ErrorType.PARSE));
            ParseStats stats = new ParseStats(0, 0, errors.size(), System.currentTimeMillis() - start);
            recordProcessing(request, checksum, stats, ProcessingStatus.FAILED);
            return new IngestionResult<>(IngestionStatus.FAILED, request.objectKey(), checksum, records, errors, warnings, stats);
        }

        MappingPlan mappingPlan = annotationIntrospector.introspect(targetType, mappingOptions);
        HeaderIndex headerIndex = new HeaderIndex(parsedCsv.headers(), mappingOptions);
        warnings.addAll(findUnknownHeaders(parsedCsv.headers(), mappingPlan, mappingOptions));
        errors.addAll(validateRequiredHeaders(mappingPlan, headerIndex, mappingOptions));

        RowMapper<T> rowMapper = new RowMapper<>(converterRegistry);
        BeanValidatorAdapter validatorAdapter = new BeanValidatorAdapter(validationOptions.beanValidationEnabled());

        long rowNumber = 2;
        for (String[] row : parsedCsv.rows()) {
            RowMappingResult<T> result = rowMapper.mapRow(row, rowNumber, mappingPlan, headerIndex, mappingOptions, targetType);
            if (!result.errors().isEmpty()) {
                errors.addAll(result.errors());
            } else if (result.mappedObject() != null) {
                Set<ConstraintViolation<T>> violations = validatorAdapter.validate(result.mappedObject());
                if (!violations.isEmpty()) {
                    errors.addAll(toValidationErrors(violations, rowNumber));
                } else {
                    records.add(result.mappedObject());
                }
            }
            rowNumber++;
        }

        ParseStats stats = new ParseStats(parsedCsv.rows().size(), records.size(), errors.size(), System.currentTimeMillis() - start);
        ProcessingStatus processingStatus = records.isEmpty() ? ProcessingStatus.FAILED : ProcessingStatus.PROCESSED;
        recordProcessing(request, checksum, stats, processingStatus);

        IngestionStatus status = records.isEmpty() ? IngestionStatus.FAILED : IngestionStatus.PROCESSED;
        return new IngestionResult<>(status, request.objectKey(), checksum, records, errors, warnings, stats);
    }

    private boolean isValidatorAvailable() {
        try {
            Class.forName("jakarta.validation.Validation");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private List<RowError> validateRequiredHeaders(MappingPlan mappingPlan, HeaderIndex headerIndex, CsvMappingOptions options) {
        List<RowError> headerErrors = new ArrayList<>();
        for (FieldMapping mapping : mappingPlan.fieldMappings()) {
            if (mapping.required() && headerIndex.indexOf(mapping.headerName(), options) == null) {
                headerErrors.add(new RowError(1, mapping.headerName(), null, "Missing required header", ErrorType.HEADER));
            }
        }
        return headerErrors;
    }

    private List<String> findUnknownHeaders(String[] rawHeaders, MappingPlan mappingPlan, CsvMappingOptions options) {
        Set<String> mapped = mappingPlan.fieldMappings().stream()
                .map(FieldMapping::headerName)
                .map(name -> normalizeHeader(name, options))
                .collect(Collectors.toSet());
        Set<String> unknown = new HashSet<>();
        if (rawHeaders != null) {
            for (String header : rawHeaders) {
                String normalized = normalizeHeader(header, options);
                if (!mapped.contains(normalized)) {
                    unknown.add(header);
                }
            }
        }
        if (unknown.isEmpty()) {
            return List.of();
        }
        return unknown.stream()
                .map(name -> "Unknown column: " + name)
                .toList();
    }

    private String normalizeHeader(String header, CsvMappingOptions options) {
        String normalized = header == null ? "" : header;
        if (options.trimHeaders()) {
            normalized = normalized.trim();
        }
        if (options.headerCaseInsensitive() || !options.exactHeaderMatch()) {
            normalized = normalized.toLowerCase();
        }
        return normalized;
    }

    private <T> List<RowError> toValidationErrors(Set<ConstraintViolation<T>> violations, long rowNumber) {
        List<RowError> validationErrors = new ArrayList<>();
        for (ConstraintViolation<T> violation : violations) {
            String column = violation.getPropertyPath() == null ? null : violation.getPropertyPath().toString();
            validationErrors.add(new RowError(
                    rowNumber,
                    column,
                    null,
                    violation.getMessage(),
                    ErrorType.VALIDATION
            ));
        }
        return validationErrors;
    }

    private void recordProcessing(CsvIngestionRequest request, String checksum, ParseStats stats, ProcessingStatus status) {
        ProcessedCsvFile processed = new ProcessedCsvFile();
        processed.setObjectKey(request.objectKey());
        processed.setChecksumSha256(checksum);
        processed.setStatus(status);
        processed.setProcessedAt(Instant.now());
        processed.setSuccessCount((int) stats.successCount());
        processed.setErrorCount((int) stats.errorCount());
        processingRegistry.record(processed);
    }
}
