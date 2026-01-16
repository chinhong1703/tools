package com.example.csvparser.adapter.univocity;

import com.example.csvparser.core.CsvFormatOptions;
import com.univocity.parsers.common.TextParsingException;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

public class UniVocityCsvReader {
    public ParsedCsv parse(byte[] content, CsvFormatOptions options) {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(true);
        settings.getFormat().setDelimiter(options.delimiter());
        settings.getFormat().setQuote(options.quote());
        settings.getFormat().setQuoteEscape(options.escape());

        CsvParser parser = new CsvParser(settings);
        Charset charset = options.charset();
        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(stripBom(content, charset)), charset)) {
            List<String[]> rows = parser.parseAll(reader);
            String[] headers = parser.getContext().headers();
            return new ParsedCsv(headers, rows);
        } catch (Exception exception) {
            if (exception instanceof TextParsingException) {
                throw (TextParsingException) exception;
            }
            throw new IllegalStateException("Failed to parse CSV", exception);
        }
    }

    private byte[] stripBom(byte[] content, Charset charset) {
        if (!charset.name().equalsIgnoreCase("UTF-8")) {
            return content;
        }
        if (content.length >= 3
                && (content[0] & 0xFF) == 0xEF
                && (content[1] & 0xFF) == 0xBB
                && (content[2] & 0xFF) == 0xBF) {
            byte[] stripped = new byte[content.length - 3];
            System.arraycopy(content, 3, stripped, 0, stripped.length);
            return stripped;
        }
        return content;
    }
}
