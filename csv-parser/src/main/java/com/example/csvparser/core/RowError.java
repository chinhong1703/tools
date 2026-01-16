package com.example.csvparser.core;

public record RowError(
        long rowNumber,
        String columnName,
        String rawValue,
        String message,
        ErrorType type
) {
}
