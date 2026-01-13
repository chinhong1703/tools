package com.example.csvgenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvEscaperTest {
    private final CsvFormatSettings settings = CsvFormatSettings.defaultSettings();

    @Test
    void escapesDelimiter() {
        String value = "a,b";
        assertEquals("\"a,b\"", CsvEscaper.escapeField(value, settings));
    }

    @Test
    void escapesQuotes() {
        String value = "He said \"hi\"";
        assertEquals("\"He said \"\"hi\"\"\"", CsvEscaper.escapeField(value, settings));
    }

    @Test
    void escapesNewline() {
        String value = "line1\nline2";
        assertEquals("\"line1\nline2\"", CsvEscaper.escapeField(value, settings));
    }

    @Test
    void handlesNull() {
        assertEquals("", CsvEscaper.escapeField(null, settings));
    }

    @Test
    void handlesEmptyString() {
        assertEquals("", CsvEscaper.escapeField("", settings));
    }

    @Test
    void quotesLeadingTrailingSpaces() {
        assertEquals("\" leading\"", CsvEscaper.escapeField(" leading", settings));
        assertEquals("\"trailing \"", CsvEscaper.escapeField("trailing ", settings));
    }
}
