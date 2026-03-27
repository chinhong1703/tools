package com.example.sfe4j.sftp.session;

import com.example.sfe4j.sftp.config.SftpConnectionProperties;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;

@Component
public class SftpSessionManager implements Closeable {

    private final SftpConnectionProperties properties;
    private final SshClient sshClient;

    public SftpSessionManager(SftpConnectionProperties properties) {
        this.properties = properties;
        this.sshClient = SshClient.setUpDefaultClient();
        configureHostVerification();
        sshClient.start();
    }

    public ClientSession openSession() throws IOException {
        try {
            ClientSession session = sshClient.connect(properties.getUsername(), properties.getHost(), properties.getPort())
                    .verify(Duration.ofMillis(properties.getConnectTimeoutMs()))
                    .getSession();
            for (KeyPair keyPair : loadKeyPairs()) {
                session.addPublicKeyIdentity(keyPair);
            }
            session.auth().verify(Duration.ofMillis(properties.getAuthTimeoutMs()));
            return session;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to establish SFTP session", e);
        }
    }

    public SftpClient openSftpClient(ClientSession session) throws IOException {
        return DefaultSftpClientFactory.INSTANCE.createSftpClient(session);
    }

    private Iterable<KeyPair> loadKeyPairs() throws IOException {
        FileKeyPairProvider provider = new FileKeyPairProvider(Path.of(properties.getPrivateKey().getPath()));
        if (properties.getPrivateKey().getPassphrase() != null && !properties.getPrivateKey().getPassphrase().isBlank()) {
            provider.setPasswordFinder(FilePasswordProvider.of(properties.getPrivateKey().getPassphrase()));
        }
        return provider.loadKeys(null);
    }

    private void configureHostVerification() {
        if (properties.isAllowInsecureHostKeyVerifier()) {
            sshClient.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
            return;
        }
        Path knownHostsPath = Path.of(properties.getKnownHosts().getPath());
        sshClient.setServerKeyVerifier(new KnownHostsServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE, knownHostsPath));
    }

    @Override
    public void close() throws IOException {
        sshClient.stop();
    }
}
