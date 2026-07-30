"""将 StreamEvent 编码为标准 SSE 文本。"""

import json
from datetime import datetime
from enum import Enum
from typing import Any

from pydantic import BaseModel

from app.schemas.stream_schema import (
    StreamEvent,
    StreamEventType,
)


class SseEncoder:
    """负责生成符合 text/event-stream 格式的文本。"""

    @staticmethod
    def encode(
        event: StreamEvent,
    ) -> str:
        """将事件编码为 SSE 文本。"""
        event_name = event.event.value

        # SSE event 名不能包含换行符。
        if "\n" in event_name or "\r" in event_name:
            raise ValueError(
                "SSE event name must not contain newline"
            )

        # 将 Pydantic 数据转换为可 JSON 序列化的结构。
        serializable_data = (
            SseEncoder._to_serializable(
                event.data
            )
        )

        # 使用紧凑 JSON，避免生成不必要的空格和换行。
        data_json = json.dumps(
            serializable_data,
            ensure_ascii=False,
            separators=(",", ":"),
        )

        # SSE 事件必须以两个换行符结束。
        return (
            f"event: {event_name}\n"
            f"data: {data_json}\n\n"
        )

    @staticmethod
    def create(
        event_type: StreamEventType,
        data: dict[str, Any] | None = None,
    ) -> str:
        """创建并编码一个 SSE 事件。"""
        return SseEncoder.encode(
            StreamEvent(
                event=event_type,
                data=data or {},
            )
        )

    @staticmethod
    def start(
        trace_id: int,
        conversation_id: int | None,
    ) -> str:
        """生成 start 事件。"""
        return SseEncoder.create(
            StreamEventType.START,
            {
                "traceId": trace_id,
                "conversationId": conversation_id,
            },
        )

    @staticmethod
    def route(
        intent: str,
        need_rag: bool,
        knowledge_base_id: int | None,
    ) -> str:
        """生成 route 事件。"""
        return SseEncoder.create(
            StreamEventType.ROUTE,
            {
                "intent": intent,
                "needRag": need_rag,
                "knowledgeBaseId": knowledge_base_id,
            },
        )

    @staticmethod
    def retrieval(
        candidate_count: int,
        rerank_count: int,
        context_document_count: int,
    ) -> str:
        """生成 retrieval 事件。"""
        return SseEncoder.create(
            StreamEventType.RETRIEVAL,
            {
                "candidateCount": candidate_count,
                "rerankCount": rerank_count,
                "contextDocumentCount": (
                    context_document_count
                ),
            },
        )

    @staticmethod
    def delta(content: str) -> str:
        """生成模型增量文本事件。"""
        return SseEncoder.create(
            StreamEventType.DELTA,
            {
                "content": content,
            },
        )

    @staticmethod
    def final(
        data: dict[str, Any],
    ) -> str:
        """生成最终权威结果事件。"""
        return SseEncoder.create(
            StreamEventType.FINAL,
            data,
        )

    @staticmethod
    def error(
        code: str,
        message: str,
        trace_id: int,
    ) -> str:
        """生成流式错误事件。"""
        return SseEncoder.create(
            StreamEventType.ERROR,
            {
                "code": code,
                "message": message,
                "traceId": trace_id,
            },
        )

    @staticmethod
    def done(trace_id: int) -> str:
        """生成流结束事件。"""
        return SseEncoder.create(
            StreamEventType.DONE,
            {
                "traceId": trace_id,
            },
        )

    @staticmethod
    def heartbeat(
        timestamp: datetime,
    ) -> str:
        """生成心跳事件。"""
        return SseEncoder.create(
            StreamEventType.HEARTBEAT,
            {
                "timestamp": timestamp,
            },
        )

    @staticmethod
    def _to_serializable(
        value: Any,
    ) -> Any:
        """将常用 Python 对象转换为 JSON 可序列化对象。"""
        if isinstance(value, BaseModel):
            return value.model_dump(
                mode="json",
                by_alias=True,
            )

        if isinstance(value, datetime):
            return value.isoformat()

        if isinstance(value, Enum):
            return value.value

        if isinstance(value, dict):
            return {
                str(key): SseEncoder._to_serializable(
                    item
                )
                for key, item in value.items()
            }

        if isinstance(value, (list, tuple)):
            return [
                SseEncoder._to_serializable(item)
                for item in value
            ]

        return value