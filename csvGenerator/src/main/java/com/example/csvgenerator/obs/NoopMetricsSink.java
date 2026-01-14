package com.example.csvgenerator;

import java.util.Map;

public final class NoopMetricsSink implements MetricsSink {
    @Override
    public void incrementCounter(String name, long delta, Map<String, String> tags) {
        // no-op
    }

    @Override
    public void recordTimer(String name, long durationMs, Map<String, String> tags) {
        // no-op
    }
}
