"""SQLite backed storage for captured HTTP flows."""

from __future__ import annotations

import json
from collections.abc import Iterable
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Generator

from sqlalchemy import Integer, LargeBinary, String, Text, create_engine, select, func
from sqlalchemy.engine import Engine
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column, sessionmaker


class Base(DeclarativeBase):
    """Declarative base class."""


class HTTPFlowEntry(Base):
    """ORM model representing a captured HTTP flow."""

    __tablename__ = "http_flows"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    timestamp: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    client_ip: Mapped[str] = mapped_column(String(64), nullable=False)
    method: Mapped[str] = mapped_column(String(16), nullable=False)
    url: Mapped[str] = mapped_column(Text, nullable=False)
    http_version: Mapped[str] = mapped_column(String(8), nullable=False)
    request_headers: Mapped[str] = mapped_column(Text, nullable=False)
    request_body: Mapped[bytes | None] = mapped_column(LargeBinary, nullable=True)
    response_status: Mapped[int | None] = mapped_column(Integer, nullable=True)
    response_headers: Mapped[str | None] = mapped_column(Text, nullable=True)
    response_body: Mapped[bytes | None] = mapped_column(LargeBinary, nullable=True)
    notes: Mapped[str | None] = mapped_column(Text, nullable=True)


@dataclass(slots=True)
class FlowQueryResult:
    """Convenience container for paginated query results."""

    total: int
    entries: list[HTTPFlowEntry]


class Storage:
    """High level storage interface."""

    def __init__(self, database_url: str) -> None:
        self.engine: Engine = create_engine(database_url, future=True)
        Base.metadata.create_all(self.engine)
        self._session_factory = sessionmaker(self.engine, expire_on_commit=False, class_=Session)

    @contextmanager
    def session(self) -> Generator[Session, None, None]:
        session = self._session_factory()
        try:
            yield session
            session.commit()
        except Exception:  # pragma: no cover - defensive
            session.rollback()
            raise
        finally:
            session.close()

    def save_flow(
        self,
        *,
        client_ip: str,
        method: str,
        url: str,
        http_version: str,
        request_headers: Iterable[tuple[str, str]],
        request_body: bytes | None,
        response_status: int | None,
        response_headers: Iterable[tuple[str, str]] | None,
        response_body: bytes | None,
    ) -> HTTPFlowEntry:
        """Persist a flow and return the stored record."""

        headers_json = json.dumps(list(request_headers))
        response_headers_json = json.dumps(list(response_headers)) if response_headers is not None else None
        timestamp = datetime.now(timezone.utc).isoformat()

        with self.session() as session:
            entry = HTTPFlowEntry(
                timestamp=timestamp,
                client_ip=client_ip,
                method=method,
                url=url,
                http_version=http_version,
                request_headers=headers_json,
                request_body=request_body,
                response_status=response_status,
                response_headers=response_headers_json,
                response_body=response_body,
            )
            session.add(entry)
            session.flush()
            session.refresh(entry)
            return entry

    def list_flows(
        self,
        *,
        limit: int = 100,
        offset: int = 0,
        method: str | None = None,
        status: int | None = None,
        query: str | None = None,
    ) -> FlowQueryResult:
        """Return a list of flows with pagination and optional filters."""

        with self.session() as session:
            stmt = select(HTTPFlowEntry).order_by(HTTPFlowEntry.id.desc())
            count_stmt = select(func.count()).select_from(HTTPFlowEntry)

            if method:
                method_value = method.upper()
                stmt = stmt.filter(HTTPFlowEntry.method == method_value)
                count_stmt = count_stmt.filter(HTTPFlowEntry.method == method_value)
            if status is not None:
                stmt = stmt.filter(HTTPFlowEntry.response_status == status)
                count_stmt = count_stmt.filter(HTTPFlowEntry.response_status == status)
            if query:
                like_term = f"%{query}%"
                stmt = stmt.filter(HTTPFlowEntry.url.like(like_term))
                count_stmt = count_stmt.filter(HTTPFlowEntry.url.like(like_term))

            total = session.execute(count_stmt).scalar_one()
            entries = session.execute(stmt.limit(limit).offset(offset)).scalars().all()

            return FlowQueryResult(total=total, entries=list(entries))

    def get_flow(self, entry_id: int) -> HTTPFlowEntry | None:
        """Fetch a flow by its identifier."""

        with self.session() as session:
            return session.get(HTTPFlowEntry, entry_id)

    def delete_flow(self, entry_id: int) -> bool:
        """Delete a flow by its identifier."""

        with self.session() as session:
            entry = session.get(HTTPFlowEntry, entry_id)
            if entry is None:
                return False
            session.delete(entry)
            return True

    def clear(self) -> int:
        """Remove all flow entries and return the count removed."""

        with self.session() as session:
            entries = session.execute(select(HTTPFlowEntry)).scalars().all()
            count = len(entries)
            for entry in entries:
                session.delete(entry)
            return count


__all__ = ["Storage", "HTTPFlowEntry", "FlowQueryResult"]
