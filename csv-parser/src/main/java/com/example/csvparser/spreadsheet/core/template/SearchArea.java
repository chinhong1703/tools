package com.example.csvparser.spreadsheet.core.template;

public record SearchArea(
        int startRow,
        int endRow,
        int startColumn,
        int endColumn
) {
}
