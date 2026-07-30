from enum import Enum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class StreamEventType(str, Enum):
    """Python Chat 流支持的 SSE 事件类型。"""

    START = "start"
    ROUTE = "route"
    RETRIEVAL = "retrieval"
    DELTA = "delta"
    FINAL = "final"
    ERROR = "error"
    DONE = "done"
    HEARTBEAT = "heartbeat"

class StreamEvent(BaseModel):
    """单个 SSE 事件。"""

    model_config = ConfigDict(populate_by_name=True)

    event: StreamEventType = Field(
        ...,
        description="SSE 事件类型。",
    )

    data: dict[str, Any] = Field(
        default_factory=dict,
        description="事件携带的 JSON 数据。",
    )