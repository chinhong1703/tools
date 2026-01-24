package com.example.csvparser.spreadsheet.adapter.poi;

import com.example.csvparser.spreadsheet.core.template.ColumnRange;
import com.example.csvparser.spreadsheet.core.template.DataEndCondition;
import com.example.csvparser.spreadsheet.core.template.HeaderLocator;
import com.example.csvparser.spreadsheet.core.template.LabelSearchOptions;
import com.example.csvparser.spreadsheet.core.template.TableBlock;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PoiTableReader {
    private final PoiCellExtractor cellExtractor;
    private final PoiLabelFinder labelFinder;

    public PoiTableReader(PoiCellExtractor cellExtractor, PoiLabelFinder labelFinder) {
        this.cellExtractor = cellExtractor;
        this.labelFinder = labelFinder;
    }

    public Optional<TableData> readTable(Sheet sheet, TableBlock tableBlock, FormulaEvaluator evaluator) {
        if (tableBlock == null) {
            return Optional.empty();
        }
        HeaderLocator headerLocator = tableBlock.headerLocator();
        if (headerLocator == null) {
            throw new IllegalArgumentException("TableBlock must define a header locator");
        }
        Integer headerRowNumber = resolveHeaderRow(sheet, headerLocator, evaluator);
        if (headerRowNumber == null) {
            return Optional.empty();
        }

        Row headerRow = sheet.getRow(headerRowNumber - 1);
        if (headerRow == null) {
            return Optional.empty();
        }

        ColumnRange columnRange = tableBlock.columnRange();
        int startColumnIndex = cellExtractor.columnNameToIndex(columnRange.startColumn());
        int endColumnIndex = resolveEndColumnIndex(headerRow, columnRange, startColumnIndex, evaluator);

        List<String> headers = new ArrayList<>();
        for (int col = startColumnIndex; col <= endColumnIndex; col++) {
            Cell cell = headerRow.getCell(col);
            Cell merged = cellExtractor.getMergedRegionTopLeftCell(sheet, headerRow.getRowNum(), col);
            Cell headerCell = merged != null ? merged : cell;
            headers.add(cellExtractor.getCellText(headerCell, evaluator, true));
        }

        int startRow = tableBlock.dataStartRowNumber() == null
                ? headerRowNumber + 1
                : tableBlock.dataStartRowNumber();
        DataEndCondition endCondition = tableBlock.dataEndCondition() == null
                ? DataEndCondition.defaults()
                : tableBlock.dataEndCondition();

        List<TableData.RowData> rows = new ArrayList<>();
        int currentRow = startRow;
        int maxRow = Math.min(sheet.getLastRowNum() + 1, startRow + endCondition.maxRows() - 1);
        while (currentRow <= maxRow) {
            Row row = sheet.getRow(currentRow - 1);
            if (row == null) {
                if (endCondition.stopAtFirstBlankRow()) {
                    break;
                }
                currentRow++;
                continue;
            }
            if (isBlankRow(row, startColumnIndex, endColumnIndex, evaluator)) {
                if (endCondition.stopAtFirstBlankRow()) {
                    break;
                }
                currentRow++;
                continue;
            }
            List<Cell> cells = new ArrayList<>();
            for (int col = startColumnIndex; col <= endColumnIndex; col++) {
                cells.add(row.getCell(col));
            }
            rows.add(new TableData.RowData(currentRow, cells));
            currentRow++;
        }

        return Optional.of(new TableData(headerRowNumber, headers, rows));
    }

    private Integer resolveHeaderRow(Sheet sheet, HeaderLocator headerLocator, FormulaEvaluator evaluator) {
        if (headerLocator.explicitHeaderRowNumber() != null) {
            return headerLocator.explicitHeaderRowNumber();
        }
        if (headerLocator.headerAnchorLabel() != null) {
            LabelSearchOptions options = headerLocator.labelSearchOptions() == null
                    ? LabelSearchOptions.defaults()
                    : headerLocator.labelSearchOptions();
            Optional<Cell> anchorCell = labelFinder.findLabelCell(sheet, headerLocator.headerAnchorLabel(), options, evaluator);
            if (anchorCell.isPresent()) {
                return anchorCell.get().getRowIndex() + 1;
            }
        }
        return null;
    }

    private int resolveEndColumnIndex(Row headerRow, ColumnRange columnRange, int startColumnIndex, FormulaEvaluator evaluator) {
        if (columnRange.endColumn() != null && !columnRange.endColumn().isBlank()) {
            return cellExtractor.columnNameToIndex(columnRange.endColumn());
        }
        int lastCell = Math.max(headerRow.getLastCellNum() - 1, startColumnIndex);
        int endIndex = startColumnIndex;
        for (int col = startColumnIndex; col <= lastCell; col++) {
            Cell cell = headerRow.getCell(col);
            String text = cellExtractor.getCellText(cell, evaluator, true);
            if (!text.isBlank()) {
                endIndex = col;
            }
        }
        return endIndex;
    }

    private boolean isBlankRow(Row row, int startColumnIndex, int endColumnIndex, FormulaEvaluator evaluator) {
        for (int col = startColumnIndex; col <= endColumnIndex; col++) {
            Cell cell = row.getCell(col);
            if (!cellExtractor.isBlank(cell, evaluator)) {
                return false;
            }
        }
        return true;
    }
}
