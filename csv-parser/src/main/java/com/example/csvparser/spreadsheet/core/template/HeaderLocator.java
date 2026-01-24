package com.example.csvparser.spreadsheet.core.template;

public record HeaderLocator(
        Integer explicitHeaderRowNumber,
        String headerAnchorLabel,
        LabelSearchOptions labelSearchOptions
) {
}
