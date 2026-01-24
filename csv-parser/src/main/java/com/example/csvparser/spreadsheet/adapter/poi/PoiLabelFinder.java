package com.example.csvparser.spreadsheet.adapter.poi;

import com.example.csvparser.spreadsheet.core.template.LabelSearchOptions;
import com.example.csvparser.spreadsheet.core.template.SearchArea;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.Locale;
import java.util.Optional;

public class PoiLabelFinder {
    private final PoiCellExtractor cellExtractor;

    public PoiLabelFinder(PoiCellExtractor cellExtractor) {
        this.cellExtractor = cellExtractor;
    }

    public Optional<Cell> findLabelCell(Sheet sheet, String labelText, LabelSearchOptions options, FormulaEvaluator evaluator) {
        if (sheet == null) {
            return Optional.empty();
        }
        LabelSearchOptions effectiveOptions = options == null ? LabelSearchOptions.defaults() : options;
        SearchArea area = effectiveOptions.searchArea();
        int startRow = area == null ? sheet.getFirstRowNum() : Math.max(0, area.startRow() - 1);
        int endRow = area == null ? sheet.getLastRowNum() : Math.max(startRow, area.endRow() - 1);
        int startCol = area == null ? 0 : Math.max(0, area.startColumn() - 1);
        int endCol = area == null ? Integer.MAX_VALUE : Math.max(startCol, area.endColumn() - 1);

        String normalizedLabel = normalize(labelText, effectiveOptions);

        for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            int lastColumn = row.getLastCellNum();
            if (endCol != Integer.MAX_VALUE) {
                lastColumn = Math.min(lastColumn, endCol + 1);
            }
            if (lastColumn < 0) {
                continue;
            }
            for (int colIndex = startCol; colIndex < lastColumn; colIndex++) {
                Cell cell = row.getCell(colIndex);
                Cell candidate = cell;
                if (effectiveOptions.allowMergedCells()) {
                    Cell merged = cellExtractor.getMergedRegionTopLeftCell(sheet, rowIndex, colIndex);
                    if (merged != null) {
                        candidate = merged;
                    }
                }
                if (candidate == null) {
                    continue;
                }
                String cellText = cellExtractor.getCellText(candidate, evaluator, effectiveOptions.trim());
                String normalizedCell = normalize(cellText, effectiveOptions);
                if (normalizedLabel.equals(normalizedCell)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private String normalize(String value, LabelSearchOptions options) {
        String normalized = value == null ? "" : value;
        if (options.trim()) {
            normalized = normalized.trim();
        }
        if (options.caseInsensitive()) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        return normalized;
    }
}
