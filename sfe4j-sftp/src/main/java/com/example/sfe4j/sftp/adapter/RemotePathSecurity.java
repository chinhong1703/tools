package com.example.sfe4j.sftp.adapter;

import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class RemotePathSecurity {

    public String normalizeUnixPath(String inputPath) {
        if (inputPath == null || inputPath.isBlank()) {
            return "/";
        }
        String normalized = Path.of(inputPath).normalize().toString().replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    public boolean isWithinBase(String basePath, String candidatePath) {
        String normalizedBase = normalizeUnixPath(basePath);
        String normalizedCandidate = normalizeUnixPath(candidatePath);
        if ("/".equals(normalizedBase)) {
            return true;
        }
        return normalizedCandidate.equals(normalizedBase)
                || normalizedCandidate.startsWith(normalizedBase + "/");
    }
}
