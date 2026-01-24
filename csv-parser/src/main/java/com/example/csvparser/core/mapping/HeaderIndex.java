package com.example.csvparser.core.mapping;

import com.example.csvparser.core.CsvMappingOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class HeaderIndex {
    private final Map<String, Integer> headerToIndex;
    private final List<String> headers;

    public HeaderIndex(String[] rawHeaders, CsvMappingOptions options) {
        this.headers = normalizeHeaders(rawHeaders, options);
        this.headerToIndex = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            headerToIndex.put(headers.get(i), i);
        }
    }

    public Integer indexOf(String headerName, CsvMappingOptions options) {
        String key = normalizeHeader(headerName, options);
        return headerToIndex.get(key);
    }

    public List<String> headers() {
        return headers;
    }

    private List<String> normalizeHeaders(String[] rawHeaders, CsvMappingOptions options) {
        if (rawHeaders == null) {
            return List.of();
        }
        return Arrays.stream(rawHeaders)
                .map(header -> normalizeHeader(header, options))
                .collect(Collectors.toList());
    }

    private String normalizeHeader(String header, CsvMappingOptions options) {
        String normalized = header == null ? "" : header;
        if (options.trimHeaders()) {
            normalized = normalized.trim();
        }
        if (options.headerCaseInsensitive() || !options.exactHeaderMatch()) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        return normalized;
    }
}
