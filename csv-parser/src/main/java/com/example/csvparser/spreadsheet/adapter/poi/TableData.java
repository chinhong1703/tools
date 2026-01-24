package com.example.csvparser.spreadsheet.adapter.poi;

import org.apache.poi.ss.usermodel.Cell;

import java.util.List;

public record TableData(
        int headerRowNumber,
        List<String> headers,
        List<RowData> rows
) {
    public record RowData(
            int rowNumber,
            List<Cell> cells
    ) {
    }
}
