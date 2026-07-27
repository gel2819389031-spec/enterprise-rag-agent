"""LLM client used by the chat service."""

from fastapi import HTTPException
from langchain_openai import ChatOpenAI

from app.config import get_settings
from app.schemas.chat_schema import ChatHistoryMessage


class LlmClient:
    """Call the configured OpenAI-compatible chat model."""

    def __init__(self) -> None:
        # Read runtime settings once for this client instance.
        self._settings = get_settings()

        # DashScope / Bailian is used through the OpenAI-compatible protocol,
        # so ChatOpenAI can talk to it by changing base_url and api_key.
        self._chat_model = ChatOpenAI(
            model=self._settings.llm_model,
            api_key=self._settings.llm_api_key,
            base_url=self._settings.llm_base_url,
            temperature=self._settings.llm_temperature,
            request_timeout=self._settings.llm_timeout_seconds,
        )

    def chat(self, question: str, model: str,history: list[ChatHistoryMessage]) -> str:
        """Send a single user question to the chat model."""
        if model and model != self._settings.llm_model:
            raise HTTPException(
                status_code=400,
                detail=f"Chat model mismatch: request={model}, configured={self._settings.llm_model}",
            )

        try:
            messages = [
                (
                    "system",
                    "你是一个企业级 RAG 智能助手。"
                    "当前阶段没有接入知识库检索，只能根据通用知识和对话历史回答。"
                    "如果问题需要企业私有知识库，请说明当前回答没有使用知识库上下文。",
                )
            ]

            for item in history:
                if item.role == "USER":
                    messages.append(("human", item.content))
                elif item.role == "ASSISTANT":
                    messages.append(("ai", item.content))
            messages.append(("human", question))
            response = self._chat_model.invoke(
                messages
            )
        except Exception as exc:
            raise HTTPException(status_code=502, detail=f"Call chat model failed: {exc}") from exc

        content = response.content
        if isinstance(content, str):
            return content

        return str(content)
