package com.example.sfe4j.core.fs;

import com.example.sfe4j.core.model.ExplorerEntry;
import com.example.sfe4j.core.model.ExplorerTree;
import com.example.sfe4j.core.service.Sfe4jProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "sfe4j", name = "mode", havingValue = "local")
public class LocalFileSystemAdapter implements RemoteFileSystemAdapter {

    private final Path baseDir;

    public LocalFileSystemAdapter(Sfe4jProperties properties) {
        this.baseDir = Path.of(properties.getBaseDirPath()).normalize().toAbsolutePath();
    }

    @Override
    public ExplorerTree listDirectory(String path) throws IOException {
        Path current = Path.of(path);
        List<ExplorerEntry> entries = Files.list(current)
                .map(this::toEntry)
                .sorted(Comparator.comparing(ExplorerEntry::name))
                .toList();
        return new ExplorerTree(
                current.toString(),
                current.getParent() == null ? null : current.getParent().toString(),
                entries.stream().filter(ExplorerEntry::directory).toList(),
                entries.stream().filter(e -> !e.directory()).toList()
        );
    }

    @Override
    public ExplorerEntry stat(String path) throws IOException {
        return toEntry(Path.of(path));
    }

    @Override
    public byte[] readFile(String path, int maxBytes) throws IOException {
        try (InputStream in = Files.newInputStream(Path.of(path), StandardOpenOption.READ)) {
            return in.readNBytes(maxBytes);
        }
    }

    @Override
    public InputStream openFileStream(String path) throws IOException {
        return Files.newInputStream(Path.of(path), StandardOpenOption.READ);
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(Path.of(path));
    }

    @Override
    public String normalize(String path) {
        return Path.of(path).normalize().toAbsolutePath().toString();
    }

    @Override
    public boolean isWithinBase(String path) {
        return Path.of(path).normalize().toAbsolutePath().startsWith(baseDir);
    }

    private ExplorerEntry toEntry(Path path) {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
            String attrs = permissions.toString();
            return new ExplorerEntry(
                    path.getFileName().toString(),
                    path.toString(),
                    path.toAbsolutePath().toString(),
                    Files.isDirectory(path),
                    Files.isReadable(path),
                    Files.isWritable(path),
                    Files.isExecutable(path),
                    attrs,
                    Files.isDirectory(path) ? 0 : Files.size(path),
                    Files.getLastModifiedTime(path).toInstant()
            );
        } catch (IOException e) {
            return new ExplorerEntry(path.getFileName().toString(), path.toString(), path.toString(),
                    false, false, false, false, "unavailable", 0, Instant.EPOCH);
        }
    }
}
