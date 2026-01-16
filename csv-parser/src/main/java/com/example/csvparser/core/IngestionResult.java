package com.example.csvparser.core;

import java.util.List;

public record IngestionResult<T>(
        IngestionStatus status,
        String objectKey,
        String checksumSha256,
        List<T> records,
        List<RowError> errors,
        List<String> warnings,
        ParseStats stats
) {
}
