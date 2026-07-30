"""回答后处理使用的数据模型。"""

from enum import Enum

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.trace_schema import TokenUsage


class AnswerStatus(str, Enum):
    """最终回答状态。"""

    GENERAL = "GENERAL"
    ANSWERED = "ANSWERED"
    NO_EVIDENCE = "NO_EVIDENCE"
    CLARIFICATION_REQUIRED = "CLARIFICATION_REQUIRED"
    PARTIAL = "PARTIAL"
    FAILED = "FAILED"


class AnswerPostProcessResult(BaseModel):
    """回答清理和引用校验结果。"""

    model_config = ConfigDict(populate_by_name=True)

    answer: str = Field(...)

    answer_status: AnswerStatus = Field(
        ...,
        alias="answerStatus",
    )

    used_citation_indexes: list[int] = Field(
        default_factory=list,
        alias="usedCitationIndexes",
    )

    invalid_citation_indexes: list[int] = Field(
        default_factory=list,
        alias="invalidCitationIndexes",
    )
class LlmStreamChunk(BaseModel):
    """LLM 流式响应中的一个分片。"""

    content: str = Field(
        default="",
        description="本次新增的文本内容。",
    )

    token_usage: TokenUsage = Field(
        default_factory=TokenUsage,
        description="模型返回的 Token 使用量。",
    )