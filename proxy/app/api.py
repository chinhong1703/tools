"""REST API endpoints for interacting with captured flows."""

from __future__ import annotations

import logging
from typing import Annotated

import httpx
from fastapi import APIRouter, Depends, HTTPException, Query, status

from .storage import Storage
from .utils import build_detail, build_list_item, prepare_replay_headers, represent_body

LOGGER = logging.getLogger(__name__)


def create_router(storage: Storage) -> APIRouter:
    """Create an API router bound to a storage instance."""

    router = APIRouter(prefix="/api", tags=["requests"])

    def get_storage() -> Storage:
        return storage

    @router.get("/requests")
    def list_requests(
        *,
        limit: Annotated[int, Query(ge=1, le=500)] = 100,
        offset: Annotated[int, Query(ge=0)] = 0,
        method: str | None = Query(default=None),
        status_code: int | None = Query(default=None, alias="status"),
        q: str | None = Query(default=None),
        storage: Storage = Depends(get_storage),
    ) -> dict[str, object]:
        """Return a paginated list of captured requests."""

        result = storage.list_flows(limit=limit, offset=offset, method=method, status=status_code, query=q)
        items = [build_list_item(entry) for entry in result.entries]
        return {"total": result.total, "limit": limit, "offset": offset, "items": items}

    @router.get("/requests/{entry_id}")
    def get_request(entry_id: int, storage: Storage = Depends(get_storage)) -> dict[str, object]:
        """Return details for a single captured request."""

        entry = storage.get_flow(entry_id)
        if not entry:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Flow not found")
        return build_detail(entry)

    @router.delete("/requests/{entry_id}")
    def delete_request(entry_id: int, storage: Storage = Depends(get_storage)) -> dict[str, object]:
        """Delete a captured request."""

        deleted = storage.delete_flow(entry_id)
        if not deleted:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Flow not found")
        return {"deleted": True}

    @router.post("/requests/clear")
    def clear_requests(storage: Storage = Depends(get_storage)) -> dict[str, object]:
        """Clear all captured flows."""

        removed = storage.clear()
        return {"cleared": removed}

    @router.post("/requests/{entry_id}/replay")
    async def replay_request(entry_id: int, storage: Storage = Depends(get_storage)) -> dict[str, object]:
        """Replay a previously captured request and return the live response."""

        entry = storage.get_flow(entry_id)
        if not entry:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Flow not found")

        headers = prepare_replay_headers(entry)
        content = entry.request_body if entry.request_body else None

        try:
            async with httpx.AsyncClient(follow_redirects=True, timeout=30.0) as client:
                response = await client.request(
                    entry.method,
                    entry.url,
                    headers=headers,
                    content=content,
                )
        except httpx.HTTPError as exc:
            LOGGER.exception("Replay failed for entry %s", entry_id)
            raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail=str(exc)) from exc

        response_headers = [
            (
                name.decode() if isinstance(name, bytes) else name,
                value,
            )
            for name, value in response.headers.items()
        ]
        body_repr = represent_body(response.content)
        return {
            "status": response.status_code,
            "headers": [{"name": name, "value": value} for name, value in response_headers],
            "body": body_repr,
            "url": str(response.url),
        }

    return router


__all__ = ["create_router"]
