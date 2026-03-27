# DESIGN

## Abstraction introduced

`RemoteFileSystemAdapter` decouples web/service logic from concrete file APIs. The controller layer now depends on neutral DTOs (`ExplorerEntry`, `ExplorerTree`, `FilePreview`) instead of `java.io.File`.

## Why Apache MINA SSHD

Apache MINA SSHD offers a clean Java-native model for:
- SSH session lifecycle
- private-key auth (with optional passphrase)
- SFTP client APIs for directory/file access
- known_hosts host key verification

These capabilities align with Spring Boot and reduce ad-hoc shell/CLI dependencies.

## Known tradeoffs

- `openFileStream` in the first cut buffers remote content to avoid leaking closed SFTP sessions; this simplifies correctness but is not optimal for very large downloads.
- Directory pagination is not implemented yet.
- Error handling maps common failures to friendly UI messages but can be expanded with richer error codes.

## Future improvements

- True streaming download with managed session lifetime.
- Pagination and optional sorting for huge directories.
- Additional adapters (e.g., SMB/object storage) via the same abstraction.
- Better MIME/content detection for preview decisions.
