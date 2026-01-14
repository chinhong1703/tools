package com.example.csvgenerator;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public final class StructuredLogger {
    public enum Level {
        INFO,
        WARN,
        ERROR
    }

    public void info(String message, Map<String, ?> fields) {
        log(Level.INFO, message, fields, null);
    }

    public void warn(String message, Map<String, ?> fields) {
        log(Level.WARN, message, fields, null);
    }

    public void error(String message, Map<String, ?> fields, Throwable throwable) {
        log(Level.ERROR, message, fields, throwable);
    }

    private void log(Level level, String message, Map<String, ?> fields, Throwable throwable) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(message, "message");
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("ts=" + Instant.now());
        joiner.add("level=" + level.name());
        joiner.add("msg=" + sanitize(message));
        if (fields != null) {
            for (Map.Entry<String, ?> entry : fields.entrySet()) {
                joiner.add(entry.getKey() + "=" + sanitize(String.valueOf(entry.getValue())));
            }
        }
        if (throwable != null) {
            joiner.add("error=" + sanitize(throwable.toString()));
        }
        System.out.println(joiner);
    }

    private String sanitize(String value) {
        return value.replace("\n", " ").replace("\r", " ");
    }
}
