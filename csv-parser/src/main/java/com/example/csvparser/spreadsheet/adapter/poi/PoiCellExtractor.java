package com.example.csvparser.spreadsheet.adapter.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.Locale;

public class PoiCellExtractor {
    private final DataFormatter dataFormatter;

    public PoiCellExtractor() {
        this.dataFormatter = new DataFormatter(Locale.US);
    }

    public FormulaEvaluator createFormulaEvaluator(Workbook workbook) {
        return workbook.getCreationHelper().createFormulaEvaluator();
    }

    public String getCellText(Cell cell, FormulaEvaluator evaluator, boolean trim) {
        if (cell == null) {
            return "";
        }
        String formatted = dataFormatter.formatCellValue(cell, evaluator);
        return trim ? formatted.trim() : formatted;
    }

    public boolean isBlank(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return true;
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.BLANK) {
            return true;
        }
        String text = getCellText(cell, evaluator, true);
        return text.isBlank();
    }

    public Cell getMergedRegionTopLeftCell(Sheet sheet, int rowIndex, int columnIndex) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.isInRange(rowIndex, columnIndex)) {
                if (sheet.getRow(region.getFirstRow()) == null) {
                    return null;
                }
                return sheet.getRow(region.getFirstRow()).getCell(region.getFirstColumn());
            }
        }
        return null;
    }

    public String cellAddress(Cell cell) {
        if (cell == null) {
            return "";
        }
        return new CellAddress(cell.getRowIndex(), cell.getColumnIndex()).formatAsString();
    }

    public int columnNameToIndex(String columnName) {
        String normalized = columnName.trim().toUpperCase(Locale.ROOT);
        int index = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c < 'A' || c > 'Z') {
                throw new IllegalArgumentException("Invalid column name: " + columnName);
            }
            index = index * 26 + (c - 'A' + 1);
        }
        return index - 1;
    }
}
