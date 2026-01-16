package com.yourorg.objectstore;

import java.time.Instant;

public record ObjectInfo(String key, long sizeBytes, Instant lastModified) {}
