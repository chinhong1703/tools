package com.example.csvparser.spreadsheet.core.template;

public record TableBlock(
        HeaderLocator headerLocator,
        ColumnRange columnRange,
        Integer dataStartRowNumber,
        DataEndCondition dataEndCondition,
        boolean ignoreUnknownColumns
) {
    public static TableBlock defaults(HeaderLocator headerLocator, ColumnRange columnRange) {
        return new TableBlock(headerLocator, columnRange, null, DataEndCondition.defaults(), true);
    }
}
