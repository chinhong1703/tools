package com.example.csvparser.spreadsheet.core.template;

import java.util.List;

public record SpreadsheetTemplate(
        List<SectionTemplate> sections
) {
}
