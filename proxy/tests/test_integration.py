"""Integration test exercising the proxy and API together."""

from __future__ import annotations

import socket
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Iterator

import httpx
import pytest
from fastapi.testclient import TestClient

from app.main import Settings, create_app


class SimpleHandler(BaseHTTPRequestHandler):
    """HTTP handler that returns a fixed payload."""

    protocol_version = "HTTP/1.1"

    def do_GET(self) -> None:  # noqa: N802 - required by BaseHTTPRequestHandler
        body = b"Hello via proxy"
        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args) -> None:  # noqa: A003 - method signature is defined by base class
        return


def find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


@pytest.fixture()
def http_server() -> Iterator[str]:
    port = find_free_port()
    server = ThreadingHTTPServer(("127.0.0.1", port), SimpleHandler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        yield f"http://127.0.0.1:{port}"
    finally:
        server.shutdown()
        thread.join()


@pytest.fixture()
def test_client(tmp_path: Path) -> Iterator[TestClient]:
    proxy_port = find_free_port()
    settings = Settings(
        api_host="127.0.0.1",
        api_port=0,
        proxy_host="127.0.0.1",
        proxy_port=proxy_port,
        database_url=f"sqlite:///{tmp_path / 'flows.db'}",
        log_level="info",
    )
    app = create_app(settings)
    with TestClient(app) as client:
        client.proxy_port = proxy_port  # type: ignore[attr-defined]
        yield client


def test_capture_and_replay_flow(http_server: str, test_client: TestClient) -> None:
    proxy_port = test_client.proxy_port  # type: ignore[attr-defined]
    target_url = f"{http_server}/hello"

    with httpx.Client(proxies={"http": f"http://127.0.0.1:{proxy_port}"}, timeout=5.0, trust_env=False) as client:
        response = client.get(target_url)
        assert response.status_code == 200
        assert response.text == "Hello via proxy"

    # Wait for the proxy to persist the flow
    for _ in range(20):
        data = test_client.get("/api/requests").json()
        if data["total"] > 0:
            break
        time.sleep(0.25)
    else:  # pragma: no cover - defensive
        pytest.fail("Flow was not captured in time")

    flow_id = data["items"][0]["id"]
    detail = test_client.get(f"/api/requests/{flow_id}").json()
    assert detail["response"]["status"] == 200
    assert detail["request"]["body"]["is_text"] is True

    replay = test_client.post(f"/api/requests/{flow_id}/replay").json()
    assert replay["status"] == 200
    assert replay["body"]["is_text"] is True
    assert "Hello via proxy" in replay["body"]["content"]
