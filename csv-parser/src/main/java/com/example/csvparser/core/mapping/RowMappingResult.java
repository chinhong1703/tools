package com.example.csvparser.core.mapping;

import com.example.csvparser.core.RowError;

import java.util.List;

public record RowMappingResult<T>(
        T mappedObject,
        List<RowError> errors
) {
}
