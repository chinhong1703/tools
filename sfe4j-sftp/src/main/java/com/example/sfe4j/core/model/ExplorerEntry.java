package com.example.sfe4j.core.model;

import java.time.Instant;

public record ExplorerEntry(
        String name,
        String path,
        String fullName,
        boolean directory,
        boolean readable,
        boolean writable,
        boolean executable,
        String accessAttributes,
        long size,
        Instant modifiedAt
) {
}
