package com.example.csvgenerator;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

public record CsvGeneratorOptions(
        Path outputRoot,
        boolean enableSplitting,
        long splitSizeBytes,
        int bufferSizeBytes,
        int maxSkips,
        Clock clock
) {
    public CsvGeneratorOptions {
        Objects.requireNonNull(outputRoot, "outputRoot");
        Objects.requireNonNull(clock, "clock");
    }

    public static CsvGeneratorOptions defaultOptions(Path outputRoot) {
        return new CsvGeneratorOptions(outputRoot, false, 0L, 4 * 1024 * 1024, 100, Clock.systemUTC());
    }
}
