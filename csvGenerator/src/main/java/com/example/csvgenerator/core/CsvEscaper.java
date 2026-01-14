package com.example.csvgenerator;

import java.util.Objects;

public final class CsvEscaper {
    private CsvEscaper() {
    }

    public static String escapeField(String value, CsvFormatSettings settings) {
        Objects.requireNonNull(settings, "settings");
        if (value == null) {
            value = settings.nullPolicy().nullValue();
        }
        boolean needsQuote = needsQuote(value, settings);
        CsvQuotingPolicy policy = settings.quotingPolicy();
        boolean shouldQuote = switch (policy) {
            case ALWAYS -> true;
            case MINIMAL -> needsQuote;
            case NONE -> needsQuote; // enforce correctness when special chars are present
        };
        if (!shouldQuote) {
            return value;
        }
        String escaped = value.replace("" + settings.quoteChar(), "" + settings.quoteChar() + settings.quoteChar());
        return settings.quoteChar() + escaped + settings.quoteChar();
    }

    private static boolean needsQuote(String value, CsvFormatSettings settings) {
        char delimiter = settings.delimiter();
        char quote = settings.quoteChar();
        boolean hasSpecial = value.indexOf(delimiter) >= 0
                || value.indexOf(quote) >= 0
                || value.contains("\n")
                || value.contains("\r");
        if (hasSpecial) {
            return true;
        }
        if (value.isEmpty()) {
            return false;
        }
        return Character.isWhitespace(value.charAt(0))
                || Character.isWhitespace(value.charAt(value.length() - 1));
    }
}
