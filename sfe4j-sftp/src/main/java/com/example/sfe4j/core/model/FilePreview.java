package com.example.sfe4j.core.model;

public record FilePreview(
        String path,
        String fileName,
        String content,
        boolean previewable,
        String message
) {
}
