"""Chat API.

This API exposes the first question-answer loop:
question -> real chat model -> answer.
"""

from functools import lru_cache

from fastapi import APIRouter, Depends

from app.core.response import ApiResult
from app.schemas.chat_schema import ChatData, ChatRequest
from app.services.chat_service import ChatService

router = APIRouter(prefix="/api/chat", tags=["chat"])


@lru_cache
def get_chat_service() -> ChatService:
    """Return a cached ChatService instance."""
    return ChatService()


@router.post("/completions")
def create_chat_completion(
    request: ChatRequest,
    chat_service: ChatService = Depends(get_chat_service),
) -> ApiResult[ChatData]:
    """Create a basic answer for a user question."""
    data = chat_service.answer(request)
    return ApiResult.ok(data)
