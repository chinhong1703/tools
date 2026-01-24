package com.example.csvparser.spreadsheet.core.template;

public record KeyValueFieldMapping(
        String labelText,
        RelativeCellRef valueRef,
        String targetFieldName,
        boolean required
) {
}
