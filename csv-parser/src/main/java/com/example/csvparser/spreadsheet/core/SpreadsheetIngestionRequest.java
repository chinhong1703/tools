package com.example.csvparser.spreadsheet.core;

import com.example.csvparser.core.CsvMappingOptions;
import com.example.csvparser.core.CsvValidationOptions;
import com.example.csvparser.spreadsheet.core.template.SpreadsheetTemplate;

public record SpreadsheetIngestionRequest(
        String objectKey,
        byte[] content,
        boolean forceReprocess,
        SpreadsheetOptions spreadsheetOptions,
        SpreadsheetTemplate template,
        CsvMappingOptions mappingOptions,
        CsvValidationOptions validationOptions
) {
}
