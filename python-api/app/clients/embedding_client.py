"""Embedding model client.

Client 层负责和外部模型服务交互。

当前第一版还不接真实模型，而是生成稳定的 Mock Embedding。
这样可以先跑通 Python API、Java 调 Python、pgvector 写入链路。
"""
from fastapi import HTTPException
from langchain.embeddings import init_embeddings
from langchain_openai import OpenAIEmbeddings

from app.config import get_settings


class EmbeddingClient:
    """
    langchain embedding客户端
       Client 层只负责模型调用。
    Service 层负责请求校验和响应组装
    """

    def __init__(self) -> None:
        # 读取向量维度等配置。
        self._settings = get_settings()
        # 创建 LangChain Embedding 模型对象。
        self._embedding=OpenAIEmbeddings(
            model=self._settings.embedding_model,
            api_key=self._settings.embedding_api_key,
            base_url=self._settings.embedding_base_url,
            dimensions=self._settings.embedding_dimension,
            request_timeout=self._settings.embedding_timeout_seconds,

            # 关键配置：
            # 关闭 LangChain 自动 token 化与自动按 token 分片。
            # DashScope 的 OpenAI 兼容接口要求 input 是 str 或 list[str]，
            # 如果 LangChain 传 token id 数组，会触发 contents is neither str nor list of str。
            check_embedding_ctx_length=False,
        )


    def embed_texts(self, texts: list[str], model: str) -> list[list[float]]:
          #批量生成文本向量
        if model and model!=self._settings.embedding_model:
            raise HTTPException(
                status_code=400,
                detail=(
                    "Embedding model mismatch: "
                    f"request={model}, configured={self._settings.embedding_model}"
                ),
            )

        try:
            vectors = self._embedding.embed_documents(texts)
        except Exception as exc:
            raise HTTPException(
                status_code=502,
                detail=f"Call LangChain embedding model failed: {exc}",
            ) from exc

        self._validate_vectors(vectors, len(texts))
        return vectors

    def embed_query(self, text: str) -> list[float]:
        """生成用户问题向量，后续 RAG 检索会复用。"""
        try:
            vector = self._embedding.embed_query(text)
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
        """校验单个向量维度。"""
        if len(vector) != self._settings.embedding_dimension:
            raise HTTPException(
                status_code=502,
                detail=(
                    "Embedding dimension mismatch: "
                    f"expected={self._settings.embedding_dimension}, actual={len(vector)}"
                ),
            )
