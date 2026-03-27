package com.example.sfe4j.sftp.adapter;

import com.example.sfe4j.core.model.ExplorerEntry;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SftpStatMapper {
    public ExplorerEntry toEntry(String parentPath, SftpClient.DirEntry dirEntry) {
        SftpClient.Attributes attrs = dirEntry.getAttributes();
        String name = dirEntry.getFilename();
        String currentPath = "/".equals(parentPath) ? "/" + name : parentPath + "/" + name;
        Instant modifiedAt = attrs.getModifyTime() == null ? Instant.EPOCH : attrs.getModifyTime().toInstant();
        return new ExplorerEntry(
                name,
                currentPath,
                currentPath,
                attrs.isDirectory(),
                true,
                false,
                false,
                String.format("%o", attrs.getPermissions()),
                attrs.isDirectory() ? 0 : attrs.getSize(),
                modifiedAt
        );
    }
}
