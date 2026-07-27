"""Request and response schemas for embedding APIs.

schemas 目录类似 Java 项目里的 DTO 包。
这里使用 Pydantic 定义请求体和响应体。
FastAPI 会基于这些类自动做参数校验和 Swagger 文档生成。
"""

from pydantic import BaseModel, Field


class EmbeddingRequest(BaseModel):
    """Request body for creating embeddings.

    对应 POST /api/embeddings 的 JSON 请求体。
    """

    # texts 是待向量化文本数组。
    # min_length=1 表示至少要传一条文本。
    texts: list[str] = Field(..., min_length=1, description="Texts to embed.")

    # model 是可选字段。
    # 不传时，Service 会使用 config.py 中的默认模型。
    model: str | None = Field(default=None, description="Optional embedding model name.")


class EmbeddingItem(BaseModel):
    """Embedding result for one input text.

    每一个输入文本，对应一个 EmbeddingItem。
    """

    # index 用来标记它对应 request.texts 中的第几个文本。
    index: int = Field(..., description="Index of the original input text.")

    # embedding 是向量数组。
    # 当前 Mock 版本默认返回 1536 维。
    embedding: list[float] = Field(..., description="Embedding vector.")


class EmbeddingData(BaseModel):
    """Embedding response payload.

    这是 ApiResult.data 里的真实业务数据。
    """

    # 实际使用的模型名称。
    model: str = Field(..., description="Embedding model name.")

    # 向量维度，需要和 PostgreSQL vector(1536) 保持一致。
    dimension: int = Field(..., description="Vector dimension.")

    # 批量向量化结果列表。
    items: list[EmbeddingItem] = Field(..., description="Embedding results.")
