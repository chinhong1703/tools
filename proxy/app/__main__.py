"""Run the intercepting proxy API and mitmproxy engine."""

from __future__ import annotations

import uvicorn

from .main import Settings, create_app


def main() -> None:  # pragma: no cover - exercised via cli
    settings = Settings.from_env()
    app = create_app(settings)
    uvicorn.run(app, host=settings.api_host, port=settings.api_port, log_level=settings.log_level)


if __name__ == "__main__":
    main()
