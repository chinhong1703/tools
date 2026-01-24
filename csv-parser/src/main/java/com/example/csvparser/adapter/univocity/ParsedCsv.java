package com.example.csvparser.adapter.univocity;

import java.util.List;

public record ParsedCsv(
        String[] headers,
        List<String[]> rows
) {
}
