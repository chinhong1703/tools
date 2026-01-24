package com.example.csvparser.spreadsheet.core.template;

import java.util.List;

public record KeyValueBlock(
        List<KeyValueFieldMapping> fields,
        LabelSearchOptions labelSearchOptions
) {
}
