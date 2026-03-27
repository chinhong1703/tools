package com.example.sfe4j.core.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sfe4j")
public class Sfe4jProperties {
    private String mode = "sftp";
    private String title = "Interface Server Explorer";
    private String description = "Read-only remote file browser";
    private boolean restrictToBaseDir = true;
    private String baseDirPath = "/";
    private Preview preview = new Preview();

    public static class Preview {
        private int maxBytes = 2 * 1024 * 1024;

        public int getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(int maxBytes) {
            this.maxBytes = maxBytes;
        }
    }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRestrictToBaseDir() { return restrictToBaseDir; }
    public void setRestrictToBaseDir(boolean restrictToBaseDir) { this.restrictToBaseDir = restrictToBaseDir; }
    public String getBaseDirPath() { return baseDirPath; }
    public void setBaseDirPath(String baseDirPath) { this.baseDirPath = baseDirPath; }
    public Preview getPreview() { return preview; }
    public void setPreview(Preview preview) { this.preview = preview; }
}
