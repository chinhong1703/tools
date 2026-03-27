package com.example.sfe4j.sftp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sftp")
public class SftpConnectionProperties {
    private String host;
    private int port = 22;
    private String username;
    private PrivateKey privateKey = new PrivateKey();
    private KnownHosts knownHosts = new KnownHosts();
    private long connectTimeoutMs = 5000;
    private long authTimeoutMs = 10000;
    private long sessionIdleTimeoutMs = 300000;
    private boolean allowInsecureHostKeyVerifier = false;

    public static class PrivateKey {
        private String path;
        private String passphrase;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getPassphrase() { return passphrase; }
        public void setPassphrase(String passphrase) { this.passphrase = passphrase; }
    }

    public static class KnownHosts {
        private String path;
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public PrivateKey getPrivateKey() { return privateKey; }
    public void setPrivateKey(PrivateKey privateKey) { this.privateKey = privateKey; }
    public KnownHosts getKnownHosts() { return knownHosts; }
    public void setKnownHosts(KnownHosts knownHosts) { this.knownHosts = knownHosts; }
    public long getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public long getAuthTimeoutMs() { return authTimeoutMs; }
    public void setAuthTimeoutMs(long authTimeoutMs) { this.authTimeoutMs = authTimeoutMs; }
    public long getSessionIdleTimeoutMs() { return sessionIdleTimeoutMs; }
    public void setSessionIdleTimeoutMs(long sessionIdleTimeoutMs) { this.sessionIdleTimeoutMs = sessionIdleTimeoutMs; }
    public boolean isAllowInsecureHostKeyVerifier() { return allowInsecureHostKeyVerifier; }
    public void setAllowInsecureHostKeyVerifier(boolean allowInsecureHostKeyVerifier) { this.allowInsecureHostKeyVerifier = allowInsecureHostKeyVerifier; }
}
