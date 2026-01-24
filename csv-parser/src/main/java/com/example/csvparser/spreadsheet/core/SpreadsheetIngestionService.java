package com.example.csvparser.spreadsheet.core;

import com.example.csvparser.core.IngestionResult;

public interface SpreadsheetIngestionService {
    <T> IngestionResult<T> ingest(SpreadsheetIngestionRequest request, Class<T> targetType);
}
