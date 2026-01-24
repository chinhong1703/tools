package com.example.csvparser.spreadsheet.core.template;

public record LabelSearchOptions(
        boolean trim,
        boolean caseInsensitive,
        boolean allowMergedCells,
        SearchArea searchArea
) {
    public static LabelSearchOptions defaults() {
        return new LabelSearchOptions(true, true, true, null);
    }
}
