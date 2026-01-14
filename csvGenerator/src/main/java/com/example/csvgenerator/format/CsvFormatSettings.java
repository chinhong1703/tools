package com.example.csvgenerator;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record CsvFormatSettings(
        char delimiter,
        char quoteChar,
        String lineEnding,
        Charset charset,
        CsvQuotingPolicy quotingPolicy,
        CsvNullPolicy nullPolicy
) {
    public CsvFormatSettings {
        Objects.requireNonNull(lineEnding, "lineEnding");
        Objects.requireNonNull(charset, "charset");
        Objects.requireNonNull(quotingPolicy, "quotingPolicy");
        Objects.requireNonNull(nullPolicy, "nullPolicy");
    }

    public static CsvFormatSettings defaultSettings() {
        return new CsvFormatSettings(",".charAt(0),
                '"',
                "\n",
                StandardCharsets.UTF_8,
                CsvQuotingPolicy.MINIMAL,
                CsvNullPolicy.EMPTY_STRING);
    }
}
