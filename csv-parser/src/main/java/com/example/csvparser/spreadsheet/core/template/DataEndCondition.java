package com.example.csvparser.spreadsheet.core.template;

public record DataEndCondition(
        boolean stopAtFirstBlankRow,
        int maxRows
) {
    public static DataEndCondition defaults() {
        return new DataEndCondition(true, 10_000);
    }
}
