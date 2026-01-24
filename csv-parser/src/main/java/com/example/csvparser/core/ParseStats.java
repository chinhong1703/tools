package com.example.csvparser.core;

public record ParseStats(
        long totalRows,
        long successCount,
        long errorCount,
        long durationMillis
) {
}
