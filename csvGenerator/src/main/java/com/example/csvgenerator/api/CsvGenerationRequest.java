package com.example.csvgenerator;

import java.util.Objects;

public record CsvGenerationRequest<T>(CsvFileType fileType, Iterable<T> rows) {
    public CsvGenerationRequest {
        Objects.requireNonNull(fileType, "fileType");
        Objects.requireNonNull(rows, "rows");
    }
}
