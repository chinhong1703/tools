package com.example.csvparser.spreadsheet.core.template;

public record SectionTemplate(
        String sectionName,
        SheetSelector sheetSelector,
        KeyValueBlock keyValueBlock,
        TableBlock tableBlock
) {
}
