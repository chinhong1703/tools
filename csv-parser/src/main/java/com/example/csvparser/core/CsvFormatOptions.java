package com.example.csvparser.core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public record CsvFormatOptions(
        char delimiter,
        char quote,
        char escape,
        Charset charset
) {
    public static CsvFormatOptions defaults() {
        return new CsvFormatOptions(',', '"', '"', StandardCharsets.UTF_8);
    }
}
