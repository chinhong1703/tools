package com.example.csvparser.spreadsheet.core;

import java.util.List;

public record SpreadsheetOptions(
        SheetSelectionMode sheetSelectionMode,
        List<String> sheetNames,
        List<Integer> sheetIndexes
) {
    public static SpreadsheetOptions defaults() {
        return new SpreadsheetOptions(SheetSelectionMode.TEMPLATE, List.of(), List.of());
    }
}
