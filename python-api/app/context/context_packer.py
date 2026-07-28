"""把检索到的 Chunk 拼成 LLM 上下文。"""

from langchain_core.documents import Document

from app.config import get_settings


class ContextPacker:
    """把检索到的 Chunk 拼成 LLM 上下文。"""

    def __init__(self) -> None:
        self._settings = get_settings()

    def pack(self, documents: list[Document]) -> str:
        """将检索到的文档片段拼接为 LLM 可读的上下文文本。

        每个片段包含元数据（文档 ID、片段序号、相关性评分）和正文内容。
        当累计字符数超过配置上限时，后续片段会被截断。
        """
        if not documents:
            return ""

        parts: list[str] = []
        total_chars = 0

        for index, doc in enumerate(documents, start=1):
            content = doc.page_content.strip()
            if not content:
                continue

            score = doc.metadata.get("score")
            if score is not None:
                score_str = f"{float(score):.4f}"
            else:
                score_str = "N/A"

            block = (
                f"--- 片段 {index} ---\n"
                f"来源文档 ID: {doc.metadata.get('document_id', 'N/A')}\n"
                f"片段序号: {doc.metadata.get('chunk_index', 'N/A')}\n"
                f"相关性评分: {score_str}\n"
                f"内容:\n{content}\n"
            )

            if total_chars + len(block) > self._settings.rag_max_context_chars:
                break

            parts.append(block)
            total_chars += len(block)

        return "\n".join(parts)
