package com.example.csvparser.spreadsheet.core.mapping;

import com.example.csvparser.core.RowError;

import java.util.List;

public record SpreadsheetRowMappingResult<T>(
        T mappedObject,
        List<RowError> errors
) {
}
