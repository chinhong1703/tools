# Minimal HTTP/S Intercepting Proxy

This project provides a minimal MVP intercepting proxy and inspector built on [mitmproxy](https://mitmproxy.org/). The proxy captures HTTP and HTTPS flows, persists them to SQLite, and exposes a FastAPI-powered REST API and lightweight web UI for inspection and replay.

## Features

- MITM HTTP/S proxy using mitmproxy's addon API running inside the app process.
- SQLite persistence via SQLAlchemy capturing requests, responses, headers, and bodies.
- REST API for listing, retrieving, deleting, clearing, and replaying captured flows.
- Single-page web UI to browse flows, inspect payloads, and trigger replays.
- Replay endpoint re-sends captured requests and returns the live response without altering stored data.
- Configurable ports and database location via environment variables.
- Dockerfile for containerised runs.
- Automated tests covering storage and end-to-end proxy capture.

## Requirements

- Python 3.11+
- mitmproxy system certificates installed for HTTPS interception (see below).

## Installation

```bash
python -m venv .venv
source .venv/bin/activate
pip install -e .
```

Install optional test dependencies when needed:

```bash
pip install -e .[test]
```

## Configuration

Runtime settings are controlled via environment variables:

| Variable | Description | Default |
| --- | --- | --- |
| `PROXY_API_HOST` | API listen host | `127.0.0.1` |
| `PROXY_API_PORT` | API listen port | `8000` |
| `PROXY_LISTEN_HOST` | Proxy listen host | `0.0.0.0` |
| `PROXY_LISTEN_PORT` | Proxy listen port | `8080` |
| `PROXY_DATABASE_URL` | SQLAlchemy database URL | `sqlite:///captured.db` |
| `PROXY_LOG_LEVEL` | Uvicorn log level | `info` |

## Running

Start the proxy and API using the module entry point:

```bash
python -m app
```

The API serves the OpenAPI docs at `http://<API_HOST>:<API_PORT>/docs` and the inspector UI at the root path `/`.

### Docker

A Dockerfile is included. Build and run:

```bash
docker build -t proxy-inspector .
docker run --rm -p 8000:8000 -p 8080:8080 proxy-inspector
```

## Capturing HTTPS Traffic

1. When the proxy starts, mitmproxy generates a CA certificate under `~/.mitmproxy/`. Locate the `mitmproxy-ca-cert.pem` file.
2. Trust the certificate:
   - **macOS:**
     ```bash
     security add-trusted-cert -d -r trustRoot -k ~/Library/Keychains/login.keychain-db ~/.mitmproxy/mitmproxy-ca-cert.pem
     ```
   - **Linux (system-wide):**
     ```bash
     sudo cp ~/.mitmproxy/mitmproxy-ca-cert.pem /usr/local/share/ca-certificates/mitmproxy.crt
     sudo update-ca-certificates
     ```
3. Configure your browser or client to use the proxy host/port defined above.

## API Examples

List captured flows (default limit 100):

```bash
curl http://127.0.0.1:8000/api/requests
```

Fetch a specific flow:

```bash
curl http://127.0.0.1:8000/api/requests/1
```

Replay a flow:

```bash
curl -X POST http://127.0.0.1:8000/api/requests/1/replay
```

Delete a flow:

```bash
curl -X DELETE http://127.0.0.1:8000/api/requests/1
```

Clear all flows:

```bash
curl -X POST http://127.0.0.1:8000/api/requests/clear
```

## Testing

Run the test suite with pytest:

```bash
pytest
```

The integration test spins up the proxy, API, and a local HTTP server to ensure end-to-end capture and replay work.

## Troubleshooting

- **No HTTPS traffic captured:** Ensure the mitmproxy CA certificate is trusted by the operating system and browser. Re-run the certificate installation steps above.
- **Ports already in use:** Adjust `PROXY_API_PORT` or `PROXY_LISTEN_PORT` environment variables to unused ports before launching.
- **Binary bodies look garbled:** The API returns base64 previews for non-text payloads; download them from the API detail endpoint and decode locally if needed.
- **Replay fails with CORS errors in the UI:** If replaying against cross-origin endpoints from the browser, ensure the target allows cross-origin requests or trigger the replay from the API directly (curl/Postman).

## License

This project is provided as-is without warranty.
