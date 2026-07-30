"""RAG Trace 使用的数据模型。"""

from datetime import datetime
from enum import Enum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field
class TraceStatus(str, Enum):
    """整条 RAG Trace 的状态。"""

    RUNNING = "RUNNING"
    SUCCESS = "SUCCESS"
    DEGRADED = "DEGRADED"
    FAILED = "FAILED"

class TraceNodeStatus(str, Enum):
    """单个 Trace 节点的状态。"""

    RUNNING = "RUNNING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    SKIPPED = "SKIPPED"
class TokenUsage(BaseModel):
    """统一后的模型 Token 用量。"""

    model_config = ConfigDict(populate_by_name=True)

    input_tokens: int = Field(
        default=0,
        alias="inputTokens",
        ge=0,
    )
    output_tokens: int = Field(
        default=0,
        alias="outputTokens",
        ge=0,
    )
    total_tokens: int = Field(
        default=0,
        alias="totalTokens",
        ge=0,
    )
class TraceNode(BaseModel):
    """RAG 流程中的单个执行节点。"""

    model_config = ConfigDict(populate_by_name=True)

    name: str = Field(..., description="节点名称。")
    status: TraceNodeStatus = Field(
        default=TraceNodeStatus.RUNNING
    )
    started_at: datetime = Field(
        ...,
        alias="startedAt",
    )
    finished_at: datetime | None = Field(
        default=None,
        alias="finishedAt",
    )
    latency_ms: int | None = Field(
        default=None,
        alias="latencyMs",
    )
    input_summary: dict[str, Any] = Field(
        default_factory=dict,
        alias="inputSummary",
    )
    output_summary: dict[str, Any] = Field(
        default_factory=dict,
        alias="outputSummary",
    )
    error_message: str | None = Field(
        default=None,
        alias="errorMessage",
    )
class RagTraceData(BaseModel):
    """一次完整 RAG 请求的 Trace 数据。"""

    model_config = ConfigDict(populate_by_name=True)

    trace_id: int = Field(..., alias="traceId")
    request_id: str | None = Field(
        default=None,
        alias="requestId",
    )
    trace_type: str = Field(
        default="CHAT_QA",
        alias="traceType",
    )
    status: TraceStatus = Field(
        default=TraceStatus.RUNNING
    )
    started_at: datetime = Field(
        ...,
        alias="startedAt",
    )
    finished_at: datetime | None = Field(
        default=None,
        alias="finishedAt",
    )
    latency_ms: int | None = Field(
        default=None,
        alias="latencyMs",
    )
    input: dict[str, Any] = Field(
        default_factory=dict,
    )
    output: dict[str, Any] = Field(
        default_factory=dict,
    )
    nodes: list[TraceNode] = Field(
        default_factory=list,
    )
    token_usage: TokenUsage = Field(
        default_factory=TokenUsage,
        alias="tokenUsage",
    )
    error_message: str | None = Field(
        default=None,
        alias="errorMessage",
    )
    degraded_reasons: list[str] = Field(
        default_factory=list,
        alias="degradedReasons",
    )