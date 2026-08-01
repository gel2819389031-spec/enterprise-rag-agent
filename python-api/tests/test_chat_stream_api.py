"""Tests for the streaming chat API."""

from collections.abc import Iterator

from fastapi.testclient import TestClient

from app.api.chat_api import get_chat_service
from app.main import create_app
from app.schemas.chat_schema import ChatRequest
from app.streaming.sse_encoder import SseEncoder


class FakeChatService:
    """Return deterministic SSE events without calling external services."""

    def stream_answer(
        self,
        request: ChatRequest,
    ) -> Iterator[str]:
        yield SseEncoder.start(
            trace_id=request.trace_id,
            conversation_id=request.conversation_id,
        )
        yield SseEncoder.route(
            intent="GENERAL_CHAT",
            need_rag=False,
            knowledge_base_id=None,
        )
        yield SseEncoder.delta("你好")
        yield SseEncoder.final(
            {
                "traceId": request.trace_id,
                "question": request.question,
                "answer": "你好",
            }
        )
        yield SseEncoder.done(request.trace_id)


def test_stream_chat_endpoint_returns_sse_events() -> None:
    app = create_app()
    app.dependency_overrides[get_chat_service] = (
        lambda: FakeChatService()
    )

    with TestClient(app) as client:
        response = client.post(
            "/api/chat/stream",
            json={
                "question": "你好",
                "tenantId": 1,
                "userId": 2,
                "traceId": 1001,
                "requestId": "stream-test-001",
                "conversationId": 2001,
                "history": [],
            },
            headers={
                "Accept": "text/event-stream",
            },
        )

    assert response.status_code == 200
    assert response.headers["content-type"].startswith(
        "text/event-stream"
    )
    assert response.headers["cache-control"] == "no-cache"
    assert response.headers["x-accel-buffering"] == "no"

    body = response.text
    assert "event: start" in body
    assert "event: route" in body
    assert "event: delta" in body
    assert '"content":"你好"' in body
    assert "event: final" in body
    assert "event: done" in body

    assert body.index("event: start") < body.index(
        "event: route"
    )
    assert body.index("event: route") < body.index(
        "event: delta"
    )
    assert body.index("event: delta") < body.index(
        "event: final"
    )
    assert body.index("event: final") < body.index(
        "event: done"
    )
