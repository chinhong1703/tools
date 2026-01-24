package com.example.csvparser.core;

public interface CsvIngestionService {
    <T> IngestionResult<T> ingest(CsvIngestionRequest request, Class<T> targetType);
}
