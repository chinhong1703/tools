package com.example.sfe4j.core.model;

import java.util.List;

public record ExplorerTree(
        String currentDirectory,
        String parentDirectory,
        List<ExplorerEntry> childDirectories,
        List<ExplorerEntry> files
) {
}
