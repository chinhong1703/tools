package com.example.csvparser.spreadsheet.core.mapping;

import com.example.csvparser.core.conversion.ConversionException;
import com.example.csvparser.core.conversion.Converter;
import com.example.csvparser.core.conversion.ConverterRegistry;
import com.example.csvparser.core.mapping.FieldMapping;
import com.example.csvparser.spreadsheet.adapter.poi.PoiCellExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.CellValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SpreadsheetCellConverter {
    private final ConverterRegistry converterRegistry;
    private final PoiCellExtractor cellExtractor;

    public SpreadsheetCellConverter(ConverterRegistry converterRegistry, PoiCellExtractor cellExtractor) {
        this.converterRegistry = converterRegistry;
        this.cellExtractor = cellExtractor;
    }

    public ConversionOutcome convert(Cell cell, FieldMapping mapping, FormulaEvaluator evaluator) throws ConversionException {
        String rawValue = cellExtractor.getCellText(cell, evaluator, mapping.trim());
        if (cell == null || rawValue.isBlank()) {
            return new ConversionOutcome(null, rawValue);
        }
        Class<?> targetType = mapping.field().getType();
        CellValue evaluated = evaluateCell(cell, evaluator);
        CellType cellType = evaluated == null ? cell.getCellType() : evaluated.getCellType();

        if (targetType.equals(String.class)) {
            return new ConversionOutcome(rawValue, rawValue);
        }

        boolean numericCell = cellType == CellType.NUMERIC;
        if ((targetType.equals(LocalDate.class) || targetType.equals(LocalDateTime.class))
                && numericCell
                && DateUtil.isCellDateFormatted(cell)) {
            double numericValue = evaluated == null ? cell.getNumericCellValue() : evaluated.getNumberValue();
            LocalDateTime dateTime = DateUtil.getLocalDateTime(numericValue);
            if (targetType.equals(LocalDate.class)) {
                return new ConversionOutcome(dateTime.toLocalDate(), rawValue);
            }
            return new ConversionOutcome(dateTime, rawValue);
        }

        if (numericCell) {
            double numericValue = evaluated == null ? cell.getNumericCellValue() : evaluated.getNumberValue();
            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return new ConversionOutcome((int) numericValue, rawValue);
            }
            if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return new ConversionOutcome((long) numericValue, rawValue);
            }
            if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return new ConversionOutcome(numericValue, rawValue);
            }
            if (targetType.equals(BigDecimal.class)) {
                return new ConversionOutcome(BigDecimal.valueOf(numericValue), rawValue);
            }
        }

        if (cellType == CellType.BOOLEAN) {
            boolean booleanValue = evaluated == null ? cell.getBooleanCellValue() : evaluated.getBooleanValue();
            if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
                return new ConversionOutcome(booleanValue, rawValue);
            }
        }

        Converter<?> converter = converterRegistry.getConverter(targetType, mapping.datePatterns());
        Object converted = converter.convert(rawValue);
        return new ConversionOutcome(converted, rawValue);
    }

    public String convertCellText(Cell cell, FieldMapping mapping, FormulaEvaluator evaluator) {
        return cellExtractor.getCellText(cell, evaluator, mapping.trim());
    }

    public String cellAddress(Cell cell) {
        return cellExtractor.cellAddress(cell);
    }

    private CellValue evaluateCell(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() != CellType.FORMULA || evaluator == null) {
            return null;
        }
        return evaluator.evaluate(cell);
    }

    public record ConversionOutcome(Object value, String rawValue) {
    }
}
