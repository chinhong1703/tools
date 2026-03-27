package com.example.sfe4j.sftp.adapter;

import com.example.sfe4j.core.fs.RemoteFileSystemAdapter;
import com.example.sfe4j.core.model.ExplorerEntry;
import com.example.sfe4j.core.model.ExplorerTree;
import com.example.sfe4j.core.service.Sfe4jProperties;
import com.example.sfe4j.sftp.session.SftpSessionManager;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "sfe4j", name = "mode", havingValue = "sftp", matchIfMissing = true)
public class SftpFileSystemAdapter implements RemoteFileSystemAdapter {

    private final SftpSessionManager sessionManager;
    private final SftpStatMapper mapper;
    private final RemotePathSecurity pathSecurity;
    private final String basePath;

    public SftpFileSystemAdapter(SftpSessionManager sessionManager,
                                 SftpStatMapper mapper,
                                 RemotePathSecurity pathSecurity,
                                 Sfe4jProperties properties) {
        this.sessionManager = sessionManager;
        this.mapper = mapper;
        this.pathSecurity = pathSecurity;
        this.basePath = pathSecurity.normalizeUnixPath(properties.getBaseDirPath());
    }

    @Override
    public ExplorerTree listDirectory(String path) throws IOException {
        String normalized = normalize(path);
        List<ExplorerEntry> dirs = new ArrayList<>();
        List<ExplorerEntry> files = new ArrayList<>();
        try (ClientSession session = sessionManager.openSession();
             SftpClient sftpClient = sessionManager.openSftpClient(session)) {
            for (SftpClient.DirEntry entry : sftpClient.readDir(normalized)) {
                if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                    continue;
                }
                ExplorerEntry mapped = mapper.toEntry(normalized, entry);
                if (mapped.directory()) {
                    dirs.add(mapped);
                } else {
                    files.add(mapped);
                }
            }
        }
        dirs.sort(Comparator.comparing(ExplorerEntry::name));
        files.sort(Comparator.comparing(ExplorerEntry::name));
        String parent = "/".equals(normalized) ? null : normalized.substring(0, normalized.lastIndexOf('/'));
        if (parent != null && parent.isBlank()) {
            parent = "/";
        }
        return new ExplorerTree(normalized, parent, dirs, files);
    }

    @Override
    public ExplorerEntry stat(String path) throws IOException {
        String normalized = normalize(path);
        try (ClientSession session = sessionManager.openSession();
             SftpClient sftpClient = sessionManager.openSftpClient(session)) {
            SftpClient.Attributes attrs = sftpClient.stat(normalized);
            String name = normalized.substring(normalized.lastIndexOf('/') + 1);
            return new ExplorerEntry(name, normalized, normalized, attrs.isDirectory(), true, false, false,
                    String.format("%o", attrs.getPermissions()),
                    attrs.isDirectory() ? 0 : attrs.getSize(),
                    attrs.getModifyTime() == null ? null : attrs.getModifyTime().toInstant());
        }
    }

    @Override
    public byte[] readFile(String path, int maxBytes) throws IOException {
        try (InputStream in = openFileStream(path);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1 && total < maxBytes) {
                int toWrite = Math.min(read, maxBytes - total);
                out.write(buffer, 0, toWrite);
                total += toWrite;
                if (total >= maxBytes) break;
            }
            return out.toByteArray();
        }
    }

    @Override
    public InputStream openFileStream(String path) throws IOException {
        String normalized = normalize(path);
        try (ClientSession session = sessionManager.openSession();
             SftpClient sftpClient = sessionManager.openSftpClient(session);
             InputStream in = sftpClient.read(normalized)) {
            byte[] all = in.readAllBytes();
            return new ByteArrayInputStream(all);
        }
    }

    @Override
    public boolean exists(String path) throws IOException {
        String normalized = normalize(path);
        try (ClientSession session = sessionManager.openSession();
             SftpClient sftpClient = sessionManager.openSftpClient(session)) {
            try {
                sftpClient.stat(normalized);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    @Override
    public String normalize(String path) {
        return pathSecurity.normalizeUnixPath(path);
    }

    @Override
    public boolean isWithinBase(String path) {
        return pathSecurity.isWithinBase(basePath, path);
    }
}
