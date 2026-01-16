package com.example.csvparser.core;

import java.util.List;

public record CsvMappingOptions(
        boolean trimHeaders,
        boolean exactHeaderMatch,
        boolean ignoreUnknownColumns,
        List<String> datePatterns
) {
    public static CsvMappingOptions defaults() {
        return new CsvMappingOptions(
                true,
                true,
                true,
                List.of(
                        "yyyyMMdd",
                        "dd-MM-yyyy",
                        "dd/MM/yyyy",
                        "yyyy-MM-dd",
                        "yyyy-MM-dd HH:mm:ss",
                        "dd-MM-yyyy HH:mm:ss",
                        "dd/MM/yyyy HH:mm:ss"
                )
        );
    }
}
