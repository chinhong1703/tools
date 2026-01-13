package com.example.csvgenerator;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public record OutputNamingConfig(
        String directoryName,
        String baseName,
        String prefix,
        String suffix,
        boolean includeTimestamp,
        String timestampPattern
) {
    public OutputNamingConfig {
        Objects.requireNonNull(directoryName, "directoryName");
        Objects.requireNonNull(baseName, "baseName");
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(suffix, "suffix");
        Objects.requireNonNull(timestampPattern, "timestampPattern");
    }

    public static OutputNamingConfig defaultConfig(String directoryName, String baseName) {
        return new OutputNamingConfig(directoryName, baseName, "", "", false, "yyyyMMddHHmmss");
    }

    public String buildBaseFileName(Clock clock) {
        StringBuilder name = new StringBuilder();
        name.append(prefix).append(baseName);
        if (includeTimestamp) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timestampPattern);
            name.append("_").append(formatter.format(ZonedDateTime.now(clock)));
        }
        name.append(suffix);
        return name.toString();
    }
}
