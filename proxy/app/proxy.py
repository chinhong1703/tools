"""mitmproxy integration for capturing flows."""

from __future__ import annotations

import asyncio
import logging
import threading

from mitmproxy import http, options
from mitmproxy.tools.dump import DumpMaster

from .storage import Storage

LOGGER = logging.getLogger(__name__)


class CaptureAddon:
    """mitmproxy addon that persists HTTP flows."""

    def __init__(self, storage: Storage) -> None:
        self.storage = storage

    def response(self, flow: http.HTTPFlow) -> None:  # pragma: no cover - exercised in integration tests
        try:
            request_headers = list(flow.request.headers.items(multi=True))
            response_headers = list(flow.response.headers.items(multi=True)) if flow.response else None
            request_body = flow.request.raw_content if flow.request.raw_content else None
            response_body = flow.response.raw_content if flow.response and flow.response.raw_content else None
            client_ip = flow.client_conn.address[0] if flow.client_conn.address else "unknown"
            status_code = flow.response.status_code if flow.response else None

            self.storage.save_flow(
                client_ip=client_ip,
                method=flow.request.method,
                url=flow.request.pretty_url,
                http_version=flow.request.http_version,
                request_headers=request_headers,
                request_body=request_body,
                response_status=status_code,
                response_headers=response_headers,
                response_body=response_body,
            )
        except Exception:  # pragma: no cover - defensive
            LOGGER.exception("Failed to persist captured flow")


class ProxyRunner:
    """Wrapper around DumpMaster to manage proxy lifecycle."""

    def __init__(self, storage: Storage, *, listen_host: str, listen_port: int) -> None:
        self.storage = storage
        self.options = options.Options(
            listen_host=listen_host,
            listen_port=listen_port,
            http2=True,
            ssl_insecure=False,
            upstream_cert=True,
        )
        self.master = DumpMaster(self.options, with_termlog=False, with_dumper=False)
        self.master.addons.add(CaptureAddon(storage))
        self._thread: threading.Thread | None = None

    def start(self) -> None:
        """Start the proxy in a background thread."""

        if self._thread and self._thread.is_alive():
            return

        def run_master() -> None:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            try:
                self.master.run()
            except Exception:  # pragma: no cover - defensive
                LOGGER.exception("Proxy master encountered an error")
            finally:
                loop.close()

        self._thread = threading.Thread(target=run_master, name="mitmproxy-master", daemon=True)
        self._thread.start()

    def shutdown(self) -> None:
        """Shutdown the proxy and wait for the background thread."""

        if self.master:
            self.master.shutdown()
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=5)


__all__ = ["ProxyRunner", "CaptureAddon"]
