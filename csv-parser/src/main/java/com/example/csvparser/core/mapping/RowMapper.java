package com.example.csvparser.core.mapping;

import com.example.csvparser.core.CsvMappingOptions;
import com.example.csvparser.core.ErrorType;
import com.example.csvparser.core.RowError;
import com.example.csvparser.core.conversion.ConversionException;
import com.example.csvparser.core.conversion.Converter;
import com.example.csvparser.core.conversion.ConverterRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class RowMapper<T> {
    private final ConverterRegistry converterRegistry;

    public RowMapper(ConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    public RowMappingResult<T> mapRow(
            String[] row,
            long rowNumber,
            MappingPlan mappingPlan,
            HeaderIndex headerIndex,
            CsvMappingOptions options,
            Class<T> targetType
    ) {
        List<RowError> errors = new ArrayList<>();
        T instance = createInstance(targetType, errors, rowNumber);
        if (instance == null) {
            return new RowMappingResult<>(null, errors);
        }

        for (FieldMapping fieldMapping : mappingPlan.fieldMappings()) {
            Integer index = headerIndex.indexOf(fieldMapping.headerName(), options);
            if (index == null || index >= row.length) {
                if (fieldMapping.required()) {
                    errors.add(new RowError(
                            rowNumber,
                            fieldMapping.headerName(),
                            null,
                            "Missing required column",
                            ErrorType.CONVERSION
                    ));
                }
                continue;
            }
            String rawValue = row[index];
            if (rawValue != null && fieldMapping.trim()) {
                rawValue = rawValue.trim();
            }
            if (rawValue == null || rawValue.isBlank()) {
                if (fieldMapping.required()) {
                    errors.add(new RowError(
                            rowNumber,
                            fieldMapping.headerName(),
                            rawValue,
                            "Missing required value",
                            ErrorType.CONVERSION
                    ));
                }
                continue;
            }
            try {
                Converter<?> converter = converterRegistry.getConverter(fieldMapping.field().getType(), fieldMapping.datePatterns());
                Object converted = converter.convert(rawValue);
                setField(instance, fieldMapping.field(), converted);
            } catch (ConversionException | IllegalArgumentException exception) {
                errors.add(new RowError(
                        rowNumber,
                        fieldMapping.headerName(),
                        rawValue,
                        exception.getMessage(),
                        ErrorType.CONVERSION
                ));
            }
        }

        if (!errors.isEmpty()) {
            return new RowMappingResult<>(null, errors);
        }
        return new RowMappingResult<>(instance, errors);
    }

    private T createInstance(Class<T> targetType, List<RowError> errors, long rowNumber) {
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
                    "Failed to instantiate target type: " + exception.getMessage(),
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
}
