package com.example.csvparser.spreadsheet.core.template;

public record RelativeCellRef(
        int rowOffset,
        int colOffset
) {
}
