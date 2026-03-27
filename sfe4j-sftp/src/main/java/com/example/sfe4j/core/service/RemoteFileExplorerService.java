package com.example.sfe4j.core.service;

import com.example.sfe4j.core.fs.RemoteFileSystemAdapter;
import com.example.sfe4j.core.model.ExplorerEntry;
import com.example.sfe4j.core.model.ExplorerTree;
import com.example.sfe4j.core.model.FilePreview;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class RemoteFileExplorerService implements FileExplorerUseCase {
    private final RemoteFileSystemAdapter adapter;
    private final Sfe4jProperties properties;
    private final PreviewPolicy previewPolicy;

    public RemoteFileExplorerService(RemoteFileSystemAdapter adapter, Sfe4jProperties properties, PreviewPolicy previewPolicy) {
        this.adapter = adapter;
        this.properties = properties;
        this.previewPolicy = previewPolicy;
    }

    public ExplorerTree listDirectory(String path) {
        String normalized = resolvePath(path);
        try {
            return adapter.listDirectory(normalized);
        } catch (IOException e) {
            throw new RemoteAccessException("Unable to list directory: " + normalized, e);
        }
    }

    public FilePreview previewFile(String path) {
        String normalized = resolvePath(path);
        ExplorerEntry entry = stat(normalized);
        if (entry.directory()) {
            return new FilePreview(normalized, entry.name(), null, false, "Cannot preview a directory.");
        }
        if (!previewPolicy.isPreviewable(entry.name())) {
            return new FilePreview(normalized, entry.name(), null, false, "Binary or unsupported file type for inline preview.");
        }
        int maxBytes = properties.getPreview().getMaxBytes();
        if (entry.size() > maxBytes) {
            return new FilePreview(normalized, entry.name(), null, false,
                    "File too large for inline preview. Download the file instead.");
        }
        try {
            byte[] content = adapter.readFile(normalized, maxBytes);
            return new FilePreview(normalized, entry.name(), new String(content, StandardCharsets.UTF_8), true, null);
        } catch (IOException e) {
            throw new RemoteAccessException("Unable to preview file: " + normalized, e);
        }
    }

    public InputStream openFileStream(String path) {
        String normalized = resolvePath(path);
        try {
            return adapter.openFileStream(normalized);
        } catch (IOException e) {
            throw new RemoteAccessException("Unable to stream file: " + normalized, e);
        }
    }

    public ExplorerEntry stat(String path) {
        try {
            return adapter.stat(resolvePath(path));
        } catch (IOException e) {
            throw new RemoteAccessException("Unable to read file metadata: " + path, e);
        }
    }

    public String resolvePath(String path) {
        String requested = (path == null || path.isBlank()) ? properties.getBaseDirPath() : path;
        String normalized = adapter.normalize(requested);
        if (properties.isRestrictToBaseDir() && !adapter.isWithinBase(normalized)) {
            throw new PathOutsideBaseDirectoryException("Requested path is outside configured base directory.");
        }
        return normalized;
    }

    public String title() {
        return properties.getTitle();
    }

    public String description() {
        return properties.getDescription();
    }
}
