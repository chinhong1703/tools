"""Utility helpers for body and header conversions."""

from __future__ import annotations

import base64
import json
from typing import Iterable

from .storage import HTTPFlowEntry

BODY_PREVIEW_LIMIT = 1024


def parse_headers(headers_json: str | None) -> list[tuple[str, str]]:
    """Parse a header JSON string back into a list of tuples."""

    if not headers_json:
        return []
    try:
        raw = json.loads(headers_json)
    except json.JSONDecodeError:
        return []
    return [(str(name), str(value)) for name, value in raw]


def headers_to_dicts(headers: Iterable[tuple[str, str]]) -> list[dict[str, str]]:
    """Convert headers to serialisable dictionary form."""

    return [{"name": name, "value": value} for name, value in headers]


def represent_body(body: bytes | None, *, limit: int | None = None) -> dict[str, object]:
    """Represent a body payload for API responses."""

    if body is None:
        return {"size": 0, "is_text": True, "content": None, "encoding": "utf-8", "truncated": False}

    size = len(body)
    try:
        text = body.decode("utf-8")
        truncated = False
        content = text
        if limit is not None and len(text) > limit:
            content = text[:limit]
            truncated = True
        return {
            "size": size,
            "is_text": True,
            "content": content,
            "encoding": "utf-8",
            "truncated": truncated,
        }
    except UnicodeDecodeError:
        encoded = base64.b64encode(body).decode("ascii")
        truncated = False
        content = encoded
        if limit is not None and len(encoded) > limit:
            content = encoded[:limit]
            truncated = True
        return {
            "size": size,
            "is_text": False,
            "content": content,
            "encoding": "base64",
            "truncated": truncated,
        }


def build_list_item(entry: HTTPFlowEntry) -> dict[str, object]:
    """Serialise an entry for the list endpoint."""

    request_body = represent_body(entry.request_body, limit=BODY_PREVIEW_LIMIT)
    response_body = represent_body(entry.response_body, limit=BODY_PREVIEW_LIMIT)
    request_headers = headers_to_dicts(parse_headers(entry.request_headers))
    response_headers = headers_to_dicts(parse_headers(entry.response_headers))

    return {
        "id": entry.id,
        "timestamp": entry.timestamp,
        "client_ip": entry.client_ip,
        "method": entry.method,
        "url": entry.url,
        "http_version": entry.http_version,
        "response_status": entry.response_status,
        "request": {
            "headers": request_headers,
            "body": request_body,
        },
        "response": {
            "headers": response_headers,
            "body": response_body,
        },
        "size": (entry.request_body and len(entry.request_body) or 0)
        + (entry.response_body and len(entry.response_body) or 0),
    }


def build_detail(entry: HTTPFlowEntry) -> dict[str, object]:
    """Serialise an entry for the detail endpoint."""

    request_headers = headers_to_dicts(parse_headers(entry.request_headers))
    response_headers = headers_to_dicts(parse_headers(entry.response_headers))

    return {
        "id": entry.id,
        "timestamp": entry.timestamp,
        "client_ip": entry.client_ip,
        "method": entry.method,
        "url": entry.url,
        "http_version": entry.http_version,
        "notes": entry.notes,
        "request": {
            "headers": request_headers,
            "body": represent_body(entry.request_body),
        },
        "response": {
            "status": entry.response_status,
            "headers": response_headers,
            "body": represent_body(entry.response_body),
        },
    }


def prepare_replay_headers(entry: HTTPFlowEntry) -> list[tuple[str, str]]:
    """Prepare headers for replaying a request."""

    headers = []
    for name, value in parse_headers(entry.request_headers):
        if name.lower() in {"content-length", "host"}:
            continue
        headers.append((name, value))
    return headers
