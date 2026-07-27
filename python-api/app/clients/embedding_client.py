"""Embedding model client.

Client 层负责和外部模型服务交互。

当前第一版还不接真实模型，而是生成稳定的 Mock Embedding。
这样可以先跑通 Python API、Java 调 Python、pgvector 写入链路。
"""

import hashlib
import random
from math import sqrt

from app.config import get_settings


class EmbeddingClient:
    """Client used to create embeddings.

    Later this class can be extended to call OpenAI-compatible, DashScope,
    Jina, BGE, or local embedding services.
    """

    def __init__(self) -> None:
        # 读取向量维度等配置。
        self._settings = get_settings()

    def embed_texts(self, texts: list[str], model: str) -> list[list[float]]:
        """Create embeddings for multiple texts.

        真实模型接入后，这里会改成 HTTP 调用模型供应商。
        当前先对每个文本生成一个 Mock 向量。
        """
        return [self._mock_embedding(text, model) for text in texts]

    def _mock_embedding(self, text: str, model: str) -> list[float]:
        """Create a stable mock vector from text and model name.

        The same text and model will always produce the same vector. This is
        better than random vectors for local tests.
        """
        # 使用 model + text 计算 hash。
        # 同样的 model 和 text 会得到同样的 seed。
        seed_bytes = hashlib.sha256(f"{model}:{text}".encode("utf-8")).digest()

        # 取前 8 个字节转换为整数，作为伪随机数种子。
        seed = int.from_bytes(seed_bytes[:8], byteorder="big", signed=False)

        # random.Random(seed) 是独立随机数生成器。
        # 只要 seed 一样，生成的随机序列就一样。
        rng = random.Random(seed)

        # 生成指定维度的向量。
        # 当前默认维度是 1536，对应数据库 embedding vector(1536)。
        vector = [
            rng.uniform(-1.0, 1.0)
            for _ in range(self._settings.embedding_dimension)
        ]

        return self._normalize(vector)

    def _normalize(self, vector: list[float]) -> list[float]:
        """Normalize vector length to 1.0.

        归一化后，向量长度为 1。
        后续做余弦相似度时会更稳定。
        """
        # 计算向量的 L2 长度。
        length = sqrt(sum(value * value for value in vector))
        if length == 0:
            return vector

        # 每个分量除以向量长度。
        # round(..., 8) 是为了让返回 JSON 不至于太长。
        return [round(value / length, 8) for value in vector]
