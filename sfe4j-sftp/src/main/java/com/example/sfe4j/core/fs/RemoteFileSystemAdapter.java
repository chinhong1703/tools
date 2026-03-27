package com.example.sfe4j.core.fs;

import com.example.sfe4j.core.model.ExplorerEntry;
import com.example.sfe4j.core.model.ExplorerTree;

import java.io.IOException;
import java.io.InputStream;

public interface RemoteFileSystemAdapter {
    ExplorerTree listDirectory(String path) throws IOException;

    ExplorerEntry stat(String path) throws IOException;

    byte[] readFile(String path, int maxBytes) throws IOException;

    InputStream openFileStream(String path) throws IOException;

    boolean exists(String path) throws IOException;

    String normalize(String path);

    boolean isWithinBase(String path);
}
