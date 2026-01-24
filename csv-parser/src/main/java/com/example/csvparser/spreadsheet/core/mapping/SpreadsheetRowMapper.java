package com.example.csvparser.spreadsheet.core.mapping;

import com.example.csvparser.core.CsvMappingOptions;
import com.example.csvparser.core.ErrorType;
import com.example.csvparser.core.RowError;
import com.example.csvparser.core.conversion.ConversionException;
import com.example.csvparser.core.mapping.FieldMapping;
import com.example.csvparser.core.mapping.HeaderIndex;
import com.example.csvparser.core.mapping.MappingPlan;
import com.example.csvparser.spreadsheet.adapter.poi.TableData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SpreadsheetRowMapper<T> {
    private final SpreadsheetCellConverter cellConverter;

    public SpreadsheetRowMapper(SpreadsheetCellConverter cellConverter) {
        this.cellConverter = cellConverter;
    }

    public SpreadsheetRowMappingResult<T> mapRow(
            TableData.RowData rowData,
            MappingPlan mappingPlan,
            HeaderIndex headerIndex,
            CsvMappingOptions options,
            Class<T> targetType,
            FormulaEvaluator evaluator,
            String sheetName,
            String contextPrefix
    ) {
        List<RowError> errors = new ArrayList<>();
        T instance = createInstance(targetType, errors, rowData.rowNumber(), contextPrefix);
        if (instance == null) {
            return new SpreadsheetRowMappingResult<>(null, errors);
        }

        for (FieldMapping fieldMapping : mappingPlan.fieldMappings()) {
            Integer index = headerIndex.indexOf(fieldMapping.headerName(), options);
            if (index == null || index >= rowData.cells().size()) {
                continue;
            }
            Cell cell = rowData.cells().get(index);
            try {
                SpreadsheetCellConverter.ConversionOutcome outcome = cellConverter.convert(cell, fieldMapping, evaluator);
                if (outcome.value() == null) {
                    if (fieldMapping.required()) {
                        String location = formatLocation(sheetName, cellConverter.cellAddress(cell));
                        errors.add(new RowError(
                                rowData.rowNumber(),
                                fieldMapping.headerName(),
                                outcome.rawValue(),
                                contextPrefix + "Missing required value" + location,
                                ErrorType.CONVERSION
                        ));
                    }
                    continue;
                }
                setField(instance, fieldMapping.field(), outcome.value());
            } catch (ConversionException | IllegalArgumentException exception) {
                String rawValue = cellConverter.convertCellText(cell, fieldMapping, evaluator);
                String location = formatLocation(sheetName, cellConverter.cellAddress(cell));
                errors.add(new RowError(
                        rowData.rowNumber(),
                        fieldMapping.headerName(),
                        rawValue,
                        contextPrefix + exception.getMessage() + location,
                        ErrorType.CONVERSION
                ));
            }
        }

        if (!errors.isEmpty()) {
            return new SpreadsheetRowMappingResult<>(null, errors);
        }
        return new SpreadsheetRowMappingResult<>(instance, errors);
    }

    private T createInstance(Class<T> targetType, List<RowError> errors, long rowNumber, String contextPrefix) {
        try {
            Constructor<T> constructor = targetType.getDeclaredConstructor();
            boolean accessible = constructor.canAccess(null);
            constructor.setAccessible(true);
            try {
                return constructor.newInstance();
            } finally {
                constructor.setAccessible(accessible);
            }
        } catch (Exception exception) {
            errors.add(new RowError(
                    rowNumber,
                    null,
                    null,
                    contextPrefix + "Failed to instantiate target type: " + exception.getMessage(),
                    ErrorType.PARSE
            ));
            return null;
        }
    }

    private void setField(Object instance, Field field, Object value) {
        boolean accessible = field.canAccess(instance);
        field.setAccessible(true);
        try {
            field.set(instance, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to set field: " + field.getName(), exception);
        } finally {
            field.setAccessible(accessible);
        }
    }

    private String formatLocation(String sheetName, String cellAddress) {
        String normalizedSheet = sheetName == null ? "" : sheetName;
        String normalizedCell = cellAddress == null ? "" : cellAddress;
        if (normalizedSheet.isBlank() && normalizedCell.isBlank()) {
            return "";
        }
        return " (sheet=" + normalizedSheet + ", cell=" + normalizedCell + ")";
    }
}
