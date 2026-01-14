package com.example.csvgenerator;

import java.util.Map;

public interface MetricsSink {
    void incrementCounter(String name, long delta, Map<String, String> tags);

    void recordTimer(String name, long durationMs, Map<String, String> tags);
}
