package com.example.sfe4j.core.service;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PreviewPolicy {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "log", "txt", "xml", "json", "csv", "yaml", "yml", "properties"
    );

    public boolean isPreviewable(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        String ext = fileName.substring(dot + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }
}
