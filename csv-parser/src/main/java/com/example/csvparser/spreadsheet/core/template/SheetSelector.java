package com.example.csvparser.spreadsheet.core.template;

public record SheetSelector(
        SheetSelectorType type,
        String sheetName,
        Integer sheetIndex
) {
    public static SheetSelector any() {
        return new SheetSelector(SheetSelectorType.ANY, null, null);
    }

    public static SheetSelector byName(String sheetName) {
        return new SheetSelector(SheetSelectorType.BY_NAME, sheetName, null);
    }

    public static SheetSelector byIndex(int sheetIndex) {
        return new SheetSelector(SheetSelectorType.BY_INDEX, null, sheetIndex);
    }
}
