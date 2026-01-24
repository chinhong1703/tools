package com.example.csvparser.core;

import java.util.List;

public record CsvMappingOptions(
        boolean trimHeaders,
        boolean exactHeaderMatch,
        boolean headerCaseInsensitive,
        boolean ignoreUnknownColumns,
        List<String> datePatterns
) {
    public static CsvMappingOptions defaults() {
        return new CsvMappingOptions(
                true,
                true,
                false,
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

    public static CsvMappingOptions spreadsheetDefaults() {
        CsvMappingOptions defaults = defaults();
        return new CsvMappingOptions(
                defaults.trimHeaders(),
                defaults.exactHeaderMatch(),
                true,
                defaults.ignoreUnknownColumns(),
                defaults.datePatterns()
        );
    }
}
