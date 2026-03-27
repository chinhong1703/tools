package com.example.sfe4j.sftp.adapter;

import com.example.sfe4j.core.model.ExplorerTree;
import com.example.sfe4j.core.service.Sfe4jProperties;
import com.example.sfe4j.sftp.config.SftpConnectionProperties;
import com.example.sfe4j.sftp.session.SftpSessionManager;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SftpFileSystemAdapterIntegrationTest {

    @TempDir
    Path tempDir;

    private SshServer server;
    private Path clientKeyPath;

    @BeforeEach
    void startServer() throws Exception {
        Path root = tempDir.resolve("remote-root");
        Files.createDirectories(root.resolve("logs"));
        Files.writeString(root.resolve("logs/app.log"), "line1\nline2");

        KeyPair userKey = SecurityUtils.getKeyPairGenerator("RSA").generateKeyPair();
        clientKeyPath = tempDir.resolve("id_rsa.pem");
        try (OutputStream out = Files.newOutputStream(clientKeyPath)) {
            OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(userKey, "test-key", null, out);
        }

        server = SshServer.setUpDefaultServer();
        server.setPort(0);
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(tempDir.resolve("host.ser")));
        server.setPublickeyAuthenticator((username, key, session) -> key.equals(userKey.getPublic()));
        server.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
        server.setFileSystemFactory(new VirtualFileSystemFactory(root));
        server.start();
    }

    @AfterEach
    void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void shouldListAndReadRemoteFiles() throws Exception {
        SftpConnectionProperties props = new SftpConnectionProperties();
        props.setHost("localhost");
        props.setPort(server.getPort());
        props.setUsername("test");
        props.getPrivateKey().setPath(clientKeyPath.toString());
        props.setAllowInsecureHostKeyVerifier(true);

        Sfe4jProperties sfe = new Sfe4jProperties();
        sfe.setBaseDirPath("/");

        try (SftpSessionManager manager = new SftpSessionManager(props)) {
            SftpFileSystemAdapter adapter = new SftpFileSystemAdapter(manager, new SftpStatMapper(), new RemotePathSecurity(), sfe);
            ExplorerTree tree = adapter.listDirectory("/logs");
            assertThat(tree.files()).hasSize(1);
            byte[] bytes = adapter.readFile("/logs/app.log", 100);
            assertThat(new String(bytes)).contains("line1");
        }
    }
}
