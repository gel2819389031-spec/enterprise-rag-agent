from enum import Enum

from pydantic import BaseModel, Field
"""问题改写、意图路由和知识库选择使用的数据模型。"""

class IntentType(str, Enum):
    """系统当前支持的用户意图。"""

    GENERAL_CHAT = "GENERAL_CHAT"
    RAG_QA = "RAG_QA"
    FOLLOW_UP = "FOLLOW_UP"
    CLARIFY = "CLARIFY"
    UNSUPPORTED = "UNSUPPORTED"
class ResolvedQuery(BaseModel):
    """多轮问题独立化结果。"""

    original_query: str = Field(..., description="用户输入的原始问题。")
    standalone_query: str = Field(..., description="脱离历史也能理解的完整问题。")
    rewritten: bool = Field(default=False, description="是否发生了问题改写。")
    reason: str | None = Field(default=None, description="执行或跳过改写的原因。")
class RouteDecision(BaseModel):
    """意图路由结果。"""

    intent: IntentType = Field(..., description="识别出的用户意图。")
    need_rag: bool = Field(..., description="是否需要进入知识库检索流程。")
    confidence: float = Field(default=1.0, ge=0, le=1, description="路由置信度。")
    reason: str = Field(..., description="路由依据。")
class KnowledgeBaseCandidate(BaseModel):
    """用户当前可用的知识库候选项。"""

    id: int = Field(..., description="知识库 ID。")
    name: str = Field(..., description="知识库名称。")
    description: str | None = Field(default=None, description="知识库描述。")
class KnowledgeBaseSelection(BaseModel):
    """知识库选择结果。"""

    knowledge_base_id: int | None = Field(default=None, description="最终选择的知识库 ID。")
    selection_type: str = Field(..., description="知识库选择方式。")
    confidence: float = Field(default=1.0, ge=0, le=1, description="选择置信度。")
    need_clarification: bool = Field(default=False, description="是否需要用户选择知识库。")
    reason: str = Field(..., description="选择依据。")
    candidates: list[KnowledgeBaseCandidate] = Field(
        default_factory=list,
        description="需要用户选择时返回的候选知识库。",
    )
class RetrievalQuery(BaseModel):
    """面向检索优化后的查询。"""
    semantic_query: str = Field(..., description="用于向量化和向量检索的语义查询。")
    keywords: list[str] = Field(default_factory=list, description="用于 BM25 的关键词。")
    alternative_queries: list[str] = Field(
        default_factory=list,
        description="用于后续多查询召回的扩展问题。",
    )