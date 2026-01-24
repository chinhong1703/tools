package com.example.csvparser.spreadsheet.core.mapping;

import com.example.csvparser.core.mapping.FieldMapping;
import org.apache.poi.ss.usermodel.Cell;

public record KeyValueCell(
        FieldMapping fieldMapping,
        Cell cell,
        String labelText,
        String sheetName,
        String cellAddress
) {
}
