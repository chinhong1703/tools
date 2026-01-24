package com.example.csvparser.spreadsheet.core.mapping;

import com.example.csvparser.core.RowError;

import com.example.csvparser.core.mapping.FieldMapping;

import java.util.List;
import java.util.Set;

public record KeyValueExtractionResult(
        List<KeyValueCell> keyValueCells,
        List<RowError> errors,
        Set<FieldMapping> mappedFields
) {
}
