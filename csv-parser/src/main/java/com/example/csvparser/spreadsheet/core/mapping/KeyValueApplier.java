package com.example.csvparser.spreadsheet.core.mapping;

import com.example.csvparser.core.ErrorType;
import com.example.csvparser.core.RowError;
import com.example.csvparser.core.conversion.ConversionException;
import com.example.csvparser.core.mapping.FieldMapping;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class KeyValueApplier {
    private final SpreadsheetCellConverter cellConverter;

    public KeyValueApplier(SpreadsheetCellConverter cellConverter) {
        this.cellConverter = cellConverter;
    }

    public List<RowError> applyKeyValues(
            Object instance,
            List<KeyValueCell> keyValueCells,
            FormulaEvaluator evaluator,
            long rowNumber,
            String contextPrefix
    ) {
        List<RowError> errors = new ArrayList<>();
        for (KeyValueCell keyValueCell : keyValueCells) {
            FieldMapping mapping = keyValueCell.fieldMapping();
            Cell cell = keyValueCell.cell();
            try {
                SpreadsheetCellConverter.ConversionOutcome outcome = cellConverter.convert(cell, mapping, evaluator);
                if (outcome.value() == null) {
                    if (mapping.required()) {
                        errors.add(new RowError(
                                rowNumber,
                                keyValueCell.labelText(),
                                outcome.rawValue(),
                                formatMessage(contextPrefix, keyValueCell, "Missing required value"),
                                ErrorType.CONVERSION
                        ));
                    }
                    continue;
                }
                setField(instance, mapping.field(), outcome.value());
            } catch (ConversionException | IllegalArgumentException exception) {
                String rawValue = cellConverter.convertCellText(cell, mapping, evaluator);
                errors.add(new RowError(
                        rowNumber,
                        keyValueCell.labelText(),
                        rawValue,
                        formatMessage(contextPrefix, keyValueCell, exception.getMessage()),
                        ErrorType.CONVERSION
                ));
            }
        }
        return errors;
    }

    private String formatMessage(String contextPrefix, KeyValueCell keyValueCell, String message) {
        String sheetName = keyValueCell.sheetName() == null ? "" : keyValueCell.sheetName();
        String cellAddress = keyValueCell.cellAddress() == null ? "" : keyValueCell.cellAddress();
        String location = "";
        if (!sheetName.isBlank() || !cellAddress.isBlank()) {
            location = " (sheet=" + sheetName + ", cell=" + cellAddress + ")";
        }
        return contextPrefix + message + location;
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
}
