package com.example.csvparser.core.mapping;

import java.util.List;

public record MappingPlan(
        List<FieldMapping> fieldMappings
) {
}
