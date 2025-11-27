"""Application configuration management."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(slots=True)
class Settings:
    """Runtime settings for the proxy service."""

    api_host: str = "127.0.0.1"
    api_port: int = 8000
    proxy_host: str = "0.0.0.0"
    proxy_port: int = 8080
    database_url: str = f"sqlite:///{Path('captured.db').absolute()}"
    log_level: str = "info"

    @classmethod
    def from_env(cls) -> "Settings":
        """Create settings from environment variables."""

        def get_env(name: str, default: str) -> str:
            return os.getenv(name, default)

        def get_int(name: str, default: int) -> int:
            value = os.getenv(name)
            if value is None:
                return default
            try:
                return int(value)
            except ValueError as exc:  # pragma: no cover - defensive
                raise ValueError(f"Environment variable {name} must be an integer") from exc

        return cls(
            api_host=get_env("PROXY_API_HOST", cls.api_host),
            api_port=get_int("PROXY_API_PORT", cls.api_port),
            proxy_host=get_env("PROXY_LISTEN_HOST", cls.proxy_host),
            proxy_port=get_int("PROXY_LISTEN_PORT", cls.proxy_port),
            database_url=get_env("PROXY_DATABASE_URL", cls.database_url),
            log_level=get_env("PROXY_LOG_LEVEL", cls.log_level),
        )


__all__ = ["Settings"]
