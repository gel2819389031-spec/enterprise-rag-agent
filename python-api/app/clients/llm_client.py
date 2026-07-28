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

    def chat(self, question: str, model: str,history: list[ChatHistoryMessage], context: str="") -> str:
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
                    "请严格遵循以下规则：\n"
                    "1. 当用户问题提供了「上下文」时，你必须基于上下文中的信息进行回答，"
                    "不得使用上下文之外的臆测信息。\n"
                    "2. 如果上下文信息不足以回答问题，请明确说明"
                    "「根据当前知识库中的资料，暂时无法完全回答这个问题」，"
                    "然后基于已有信息给出部分回答或建议。\n"
                    "3. 回答时尽量引用上下文中的具体内容，"
                    "必要时可以标注信息来源（如片段编号或文档 ID）。\n"
                    "4. 如果用户问题没有提供上下文，则基于通用知识回答，"
                    "并说明当前回答未使用企业知识库。\n"
                    "5. 保持回答专业、准确、简洁。",
                )
            ]

            for item in history:
                if item.role == "USER":
                    messages.append(("human", item.content))
                elif item.role == "ASSISTANT":
                    messages.append(("ai", item.content))

            if context:
                user_prompt = (
                    f"## 用户问题\n{question}\n\n"
                    f"## 检索到的知识库上下文\n{context}\n\n"
                    "请基于以上上下文回答用户问题。"
                )
            else:
                user_prompt = question

            messages.append(("human", user_prompt))
            response = self._chat_model.invoke(
                messages
            )

        except Exception as exc:
            raise HTTPException(status_code=502, detail=f"Call chat model failed: {exc}") from exc

        content = response.content
        if isinstance(content, str):
            return content

        return str(content)
