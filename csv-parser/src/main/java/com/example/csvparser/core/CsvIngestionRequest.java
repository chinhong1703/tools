package com.example.csvparser.core;

public record CsvIngestionRequest(
        String objectKey,
        byte[] content,
        boolean forceReprocess,
        CsvFormatOptions formatOptions,
        CsvMappingOptions mappingOptions,
        CsvValidationOptions validationOptions
) {
}
