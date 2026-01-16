package com.example.csvparser.core.mapping;

import java.lang.reflect.Field;
import java.util.List;

public record FieldMapping(
        Field field,
        String headerName,
        boolean required,
        boolean trim,
        List<String> datePatterns
) {
}
