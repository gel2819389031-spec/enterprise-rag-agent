"""Embedding model client.

Client 层负责和外部模型服务交互。

支持按模型名称缓存多个客户端实例，实现入库流水线按任务切换 Embedding 模型。
"""
from fastapi import HTTPException
from langchain_openai import OpenAIEmbeddings

from app.config import get_settings


class EmbeddingClient:
    """langchain embedding 客户端，按 model 参数动态创建/缓存客户端实例。"""

    def __init__(self) -> None:
        self._settings = get_settings()
        # 模型 → 客户端缓存，避免为同一模型重复创建实例。
        self._clients: dict[str, OpenAIEmbeddings] = {}
        # 预热默认模型客户端。
        self._get_or_create_client(self._settings.embedding_model)

    def _get_or_create_client(self, model: str) -> OpenAIEmbeddings:
        if model not in self._clients:
            self._clients[model] = OpenAIEmbeddings(
                model=model,
                api_key=self._settings.embedding_api_key,
                base_url=self._settings.embedding_base_url,
                dimensions=self._settings.embedding_dimension,
                request_timeout=self._settings.embedding_timeout_seconds,
                # 关闭 LangChain 自动 token 化与自动按 token 分片。
                check_embedding_ctx_length=False,
            )
        return self._clients[model]

    def embed_texts(self, texts: list[str], model: str | None = None) -> list[list[float]]:
        """批量生成文本向量。

        Args:
            texts: 待向量化的文本列表。
            model: 指定模型名称，None 则使用全局默认模型。
        """
        effective_model = model or self._settings.embedding_model
        client = self._get_or_create_client(effective_model)

        try:
            vectors = client.embed_documents(texts)
        except Exception as exc:
            raise HTTPException(
                status_code=502,
                detail=f"Call LangChain embedding model failed: {exc}",
            ) from exc

        self._validate_vectors(vectors, len(texts))
        return vectors

    def embed_query(self, text: str) -> list[float]:
        """生成用户问题向量，后续 RAG 检索会复用（使用默认模型）。"""
        client = self._get_or_create_client(self._settings.embedding_model)
        try:
            vector = client.embed_query(text)
        except Exception as exc:
            raise HTTPException(
                status_code=502,
                detail=f"Call LangChain query embedding failed: {exc}",
            ) from exc

        self._validate_vector(vector)
        return vector

    def _validate_vectors(self, vectors: list[list[float]], expected_size: int) -> None:
        """校验批量向量数量和每个向量维度。"""
        if len(vectors) != expected_size:
            raise HTTPException(
                status_code=502,
                detail=f"Embedding count mismatch: expected={expected_size}, actual={len(vectors)}",
            )

        for vector in vectors:
            self._validate_vector(vector)

    def _validate_vector(self, vector: list[float]) -> None:
        """校验单个向量维度与全局配置一致。"""
        if len(vector) != self._settings.embedding_dimension:
            raise HTTPException(
                status_code=502,
                detail=(
                    "Embedding dimension mismatch: "
                    f"expected={self._settings.embedding_dimension}, actual={len(vector)}"
                ),
            )
