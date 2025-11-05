"""Application factory and runtime wiring."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from .api import create_router
from .proxy import ProxyRunner
from .settings import Settings
from .storage import Storage

LOGGER = logging.getLogger(__name__)


def create_app(settings: Settings | None = None) -> FastAPI:
    """Create the FastAPI application."""

    settings = settings or Settings.from_env()
    storage = Storage(settings.database_url)
    proxy_runner = ProxyRunner(storage, listen_host=settings.proxy_host, listen_port=settings.proxy_port)
    web_dir = Path(__file__).parent / "web"

    @asynccontextmanager
    async def lifespan(app: FastAPI):  # pragma: no cover - exercised in integration tests
        LOGGER.info(
            "Starting proxy on %s:%s and API on %s:%s",
            settings.proxy_host,
            settings.proxy_port,
            settings.api_host,
            settings.api_port,
        )
        proxy_runner.start()
        try:
            yield
        finally:
            proxy_runner.shutdown()

    app = FastAPI(title="Intercepting Proxy", lifespan=lifespan)
    app.state.settings = settings
    app.state.storage = storage
    app.state.proxy_runner = proxy_runner

    app.include_router(create_router(storage))

    if web_dir.exists():
        app.mount("/static", StaticFiles(directory=web_dir), name="static")

        @app.get("/", include_in_schema=False)
        async def index() -> FileResponse:
            return FileResponse(web_dir / "index.html")

    return app


__all__ = ["create_app", "Settings"]
