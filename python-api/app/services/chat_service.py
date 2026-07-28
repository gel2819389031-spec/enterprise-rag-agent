"""Business service for the basic question-answer flow."""

from fastapi import HTTPException

from app.clients.llm_client import LlmClient
from app.config import get_settings
from app.context.context_packer import ContextPacker
from app.retriever.pgvector_retriever import PgVectorRetriever
from app.schemas.chat_schema import ChatData, ChatRequest


class ChatService:
    """Create a direct model answer for a user question."""

    def __init__(self) -> None:
        # Load global configuration.
        self._settings = get_settings()

        # Client layer owns the actual model call.
        self._client = LlmClient()
        self._retriever = PgVectorRetriever()
        self._context_packer = ContextPacker()

    def answer(self, request: ChatRequest) -> ChatData:
        """Answer the user question without retrieval for now."""
        # Validate request before calling the model.
        self._validate_request(request)

        # Use request model if present, otherwise use configured default model.
        model = request.model or self._settings.llm_model
        context = ""
        citations = []
        if request.tenant_id and request.knowledge_base_id:
            documents = self._retriever.retrieve(
                question=request.question,
                tenant_id=request.tenant_id,
                knowledge_base_id=request.knowledge_base_id,
            )

            context = self._context_packer.pack(documents)

            citations = [
                {
                    "chunk_id": doc.metadata.get("chunk_id"),
                    "document_id": doc.metadata.get("document_id"),
                    "chunk_index": doc.metadata.get("chunk_index"),
                    "score": doc.metadata.get("score"),
                }
                for doc in documents
            ]
        # Call the LLM directly. No vector search or context packing happens in this step.
        answer = self._client.chat(request.question.strip(), model, request.history,context=context,)

        # Build the unified response payload.
        return ChatData(
            question=request.question,
            answer=answer,
            model=model,
            mode="basic",
            citations=citations,
        )

    def _validate_request(self, request: ChatRequest) -> None:
        """Validate the chat request."""
        if request.question is None or not request.question.strip():
            raise HTTPException(status_code=400, detail="question must not be blank")
