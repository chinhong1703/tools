package com.example.sfe4j.core.service;

import com.example.sfe4j.core.model.ExplorerEntry;
import com.example.sfe4j.core.model.ExplorerTree;
import com.example.sfe4j.core.model.FilePreview;

import java.io.InputStream;

public interface FileExplorerUseCase {
    ExplorerTree listDirectory(String path);
    FilePreview previewFile(String path);
    InputStream openFileStream(String path);
    ExplorerEntry stat(String path);
    String title();
    String description();
}
