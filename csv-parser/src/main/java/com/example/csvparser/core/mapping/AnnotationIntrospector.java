package com.example.csvparser.core.mapping;

import com.example.csvparser.core.CsvMappingOptions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AnnotationIntrospector {
    public MappingPlan introspect(Class<?> targetType, CsvMappingOptions options) {
        List<FieldMapping> fieldMappings = new ArrayList<>();
        for (Field field : targetType.getDeclaredFields()) {
            if (field.isAnnotationPresent(CsvIgnore.class)) {
                continue;
            }
            CsvColumn column = field.getAnnotation(CsvColumn.class);
            if (column == null) {
                throw new IllegalArgumentException("Missing @CsvColumn on field: " + field.getName());
            }
            CsvTrim trim = field.getAnnotation(CsvTrim.class);
            CsvDateFormats dateFormats = field.getAnnotation(CsvDateFormats.class);
            List<String> patterns = options.datePatterns();
            if (dateFormats != null && dateFormats.value().length > 0) {
                patterns = List.of(dateFormats.value());
            }
            fieldMappings.add(new FieldMapping(
                    field,
                    column.name(),
                    column.required(),
                    trim == null || trim.value(),
                    patterns
            ));
        }
        return new MappingPlan(fieldMappings);
    }
}
