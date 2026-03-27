# sfe4j-sftp

Internal read-only SFE4J-style explorer for browsing remote directories over SFTP using username + private-key authentication.

## Architecture

- `RemoteFileSystemAdapter` provides a backend-neutral API for browsing, stat, read, and streaming files.
- `RemoteFileExplorerService` centralizes path normalization, base-directory enforcement, preview policy, and controller-facing workflow.
- `SftpFileSystemAdapter` implements SFTP behavior with Apache MINA SSHD.
- Existing endpoint style is preserved:
  - `GET /file-explorer?dir=/path`
  - `GET /file-viewer?file=/path/to/file`
  - `GET /file-downloader?file=/path/to/file`

## Configuration

```yaml
sfe4j:
  mode: sftp
  title: "Interface Server Explorer"
  description: "Read-only remote file browser"
  restrict-to-base-dir: true
  base-dir-path: "/remote/base"
  preview:
    max-bytes: 2097152

sftp:
  host: interface-server.example
  port: 22
  username: myuser
  private-key:
    path: /etc/keys/interface_id_rsa
    passphrase: ""
  known-hosts:
    path: /etc/ssh/known_hosts
  connect-timeout-ms: 5000
  auth-timeout-ms: 10000
  session-idle-timeout-ms: 300000
  allow-insecure-host-key-verifier: false
```

## Local run

```bash
mvn spring-boot:run
```

Open `http://localhost:8080/file-explorer`.

## OpenShift deployment notes (test/troubleshooting)

1. Build and deploy into a test namespace.
2. Mount private key and known_hosts into the pod.
3. Set SFTP and SFE4J configuration via env vars or mounted `application.yml`.
4. Access from workstation using:
   - `oc port-forward pod/<pod-name> 8080:8080`
   - open `http://localhost:8080/file-explorer`

No public route is required for this troubleshooting utility.

## Security limitations and behavior

- Read-only UI/service surface (list, preview, download only).
- Base-directory restriction enforced for inbound `dir` and `file` paths.
- Preview is text-only by extension and capped by `sfe4j.preview.max-bytes`.
- Host key verification defaults to known_hosts.
- `allow-insecure-host-key-verifier=true` is available only for isolated test use.
