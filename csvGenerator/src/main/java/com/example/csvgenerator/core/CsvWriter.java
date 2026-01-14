package com.example.csvgenerator;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

public final class CsvWriter implements Closeable {
    private final Writer writer;
    private final CsvFormatSettings settings;
    private long rowsWritten;

    public CsvWriter(Writer writer, CsvFormatSettings settings) {
        this.writer = Objects.requireNonNull(writer, "writer");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public void writeHeader(String[] headers) throws IOException {
        writeRowInternal(headers);
    }

    public void writeRow(String[] fields) throws IOException {
        writeRowInternal(fields);
        rowsWritten++;
    }

    public long rowsWritten() {
        return rowsWritten;
    }

    private void writeRowInternal(String[] fields) throws IOException {
        Objects.requireNonNull(fields, "fields");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                builder.append(settings.delimiter());
            }
            builder.append(CsvEscaper.escapeField(fields[i], settings));
        }
        builder.append(settings.lineEnding());
        writer.write(builder.toString());
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
