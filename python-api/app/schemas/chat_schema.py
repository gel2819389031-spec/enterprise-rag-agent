"""Request and response schemas for the basic chat API."""

from pydantic import BaseModel, Field

class ChatHistoryMessage(BaseModel):
    """One historical chat message."""

    role: str = Field(..., description="Message role: USER or ASSISTANT.")
    content: str = Field(..., description="Message content.")


class ChatRequest(BaseModel):
    """Request body for a user question.

    This first version does not run retrieval. It only sends the question
    directly to the configured chat model.
    """
    question: str = Field(..., min_length=1, description="User question.")
    model: str | None = Field(default=None, description="Optional chat model name.")
    tenant_id: int | None =  Field(default=None, alias="tenantId")
    knowledge_base_id: int | None = Field(default=None, alias="knowledgeBaseId")
    history: list[ChatHistoryMessage] = Field(default_factory=list, description="Recent conversation history.")


class ChatData(BaseModel):
    """Response payload for a basic chat answer."""

    question: str = Field(..., description="Original user question.")
    answer: str = Field(..., description="Model answer.")
    model: str = Field(..., description="Actual model used.")
    mode: str = Field(default="basic", description="Chat mode.")
    citations: list[dict] = Field(default_factory=list, description="List of document chunks used for context.")
