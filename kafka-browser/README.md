# Kafka Browser

Minimal Kafka “topic viewer + producer” backed by a Node.js WebSocket server.

## Requirements

- Node.js v20+
- A Kafka broker accessible from the server

## Install & Run

```bash
npm install
node server.js
```

Open http://localhost:3000 in your browser (or the port you set via `PORT`).

> **Security note:** This server binds to `127.0.0.1` by default. Do not expose it publicly because credentials are sent over WebSocket and kept only in memory.

## TLS / mTLS handling

KafkaJS expects PEM materials (CA chain, client cert, client key). The backend converts uploaded keystore/truststore files into PEM in memory.

Supported types:

- **PKCS12 (.p12 / .pfx)**: parsed with `node-forge`.
- **JKS**: parsed with `jks-js`.

If JKS parsing fails and `keytool` is available on your system, the server will attempt to convert the JKS file to PKCS12 using `keytool` as a fallback. The conversion runs in a temporary directory and is removed immediately after loading.

## Limitations

- Browsers cannot connect directly to Kafka (Kafka uses raw TCP). This app relies on the Node.js backend to connect to Kafka and stream messages to the browser over WebSocket.
- Credentials are held in memory only; they are never written to disk.
