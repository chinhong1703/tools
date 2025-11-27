"""Tests for the SQLite storage layer."""

from __future__ import annotations

from pathlib import Path

import pytest

from app.storage import Storage


@pytest.fixture()
def temp_storage(tmp_path: Path) -> Storage:
    db_path = tmp_path / "test.db"
    return Storage(f"sqlite:///{db_path}")


def test_save_and_retrieve_flow(temp_storage: Storage) -> None:
    entry = temp_storage.save_flow(
        client_ip="127.0.0.1",
        method="GET",
        url="http://example.com/",
        http_version="HTTP/1.1",
        request_headers=[("Host", "example.com")],
        request_body=b"",
        response_status=200,
        response_headers=[("Content-Type", "text/plain")],
        response_body=b"ok",
    )

    assert entry.id is not None

    fetched = temp_storage.get_flow(entry.id)
    assert fetched is not None
    assert fetched.method == "GET"
    assert fetched.response_status == 200

    listing = temp_storage.list_flows()
    assert listing.total == 1
    assert listing.entries[0].id == entry.id


def test_delete_and_clear(temp_storage: Storage) -> None:
    entry1 = temp_storage.save_flow(
        client_ip="127.0.0.1",
        method="GET",
        url="http://example.com/",
        http_version="HTTP/1.1",
        request_headers=[],
        request_body=None,
        response_status=200,
        response_headers=[],
        response_body=None,
    )
    entry2 = temp_storage.save_flow(
        client_ip="127.0.0.1",
        method="POST",
        url="http://example.com/",
        http_version="HTTP/1.1",
        request_headers=[],
        request_body=b"data",
        response_status=201,
        response_headers=[],
        response_body=None,
    )

    assert temp_storage.delete_flow(entry1.id)
    assert not temp_storage.delete_flow(9999)

    remaining = temp_storage.list_flows()
    assert remaining.total == 1
    assert remaining.entries[0].id == entry2.id

    cleared = temp_storage.clear()
    assert cleared == 1
    assert temp_storage.list_flows().total == 0
