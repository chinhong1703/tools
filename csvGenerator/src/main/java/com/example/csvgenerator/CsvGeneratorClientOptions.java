package com.example.csvgenerator;

import java.util.Objects;

public record CsvGeneratorClientOptions(int maxParallelism, int queueSize) {
    public CsvGeneratorClientOptions {
        if (maxParallelism < 1) {
            throw new IllegalArgumentException("maxParallelism must be >= 1");
        }
        if (queueSize < 1) {
            throw new IllegalArgumentException("queueSize must be >= 1");
        }
    }

    public static CsvGeneratorClientOptions defaultOptions() {
        return new CsvGeneratorClientOptions(1, 100);
    }
}
