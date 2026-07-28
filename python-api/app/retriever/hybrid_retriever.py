"""向量检索、关键词检索和 RRF 融合的统一入口。"""

import logging

from langchain_core.documents import Document

from app.config import get_settings
from app.retriever.keyword_retriever import KeywordRetriever
from app.retriever.pgvector_retriever import PgVectorRetriever
from app.retriever.rrf_fusion import RrfFusion
from app.schemas.retrieval_schema import RetrievalCandidate


logger = logging.getLogger(__name__)


class HybridRetriever:
    """编排向量检索、关键词检索和结果融合。"""

    def __init__(self) -> None:
        """初始化两路检索器和融合器。"""
        self._settings = get_settings()
        self._vector_retriever = PgVectorRetriever()
        self._keyword_retriever = KeywordRetriever()
        self._rrf_fusion = RrfFusion(
            rrf_k=self._settings.retrieval_rrf_k
        )

    def retrieve(
        self,
        semantic_query: str,
        keywords: list[str],
        tenant_id: int,
        knowledge_base_id: int,
    ) -> list[Document]:
        """执行混合检索并返回 LangChain Document。"""
        vector_candidates: list[RetrievalCandidate] = []
        keyword_candidates: list[RetrievalCandidate] = []

        vector_error: Exception | None = None
        keyword_error: Exception | None = None

        try:
            # 执行向量语义检索。
            vector_candidates = self._vector_retriever.retrieve(
                question=semantic_query,
                tenant_id=tenant_id,
                knowledge_base_id=knowledge_base_id,
                top_k=self._settings.retrieval_vector_top_k,
            )
        except Exception as exception:
            vector_error = exception
            logger.exception(
                "向量检索失败, tenant_id=%s, knowledge_base_id=%s",
                tenant_id,
                knowledge_base_id,
            )

        if self._settings.retrieval_enable_keyword and keywords:
            try:
                # 执行关键词精确检索。
                keyword_candidates = self._keyword_retriever.retrieve(
                    keywords=keywords,
                    tenant_id=tenant_id,
                    knowledge_base_id=knowledge_base_id,
                    top_k=self._settings.retrieval_keyword_top_k,
                )
            except Exception as exception:
                keyword_error = exception
                logger.exception(
                    "关键词检索失败, tenant_id=%s, knowledge_base_id=%s",
                    tenant_id,
                    knowledge_base_id,
                )

        # 两路都失败时不能继续生成无依据回答。
        if vector_error is not None and keyword_error is not None:
            raise RuntimeError("Vector and keyword retrieval both failed") from vector_error

        # 关键词检索未启用时，向量检索失败也无法降级。
        if (
            vector_error is not None
            and not self._settings.retrieval_enable_keyword
        ):
            raise RuntimeError("Vector retrieval failed") from vector_error

        # 关键词为空且向量检索失败时同样无法降级。
        if vector_error is not None and not keywords:
            raise RuntimeError("Vector retrieval failed and keywords are empty") from vector_error

        # 对两路候选结果执行 RRF 排名融合。
        fused_candidates = self._rrf_fusion.fuse(
            vector_candidates=vector_candidates,
            keyword_candidates=keyword_candidates,
            final_top_k=self._settings.retrieval_final_top_k,
        )

        # 转换为现有 ContextPacker 支持的 Document。
        return [
            self._to_document(candidate)
            for candidate in fused_candidates
        ]

    @staticmethod
    def _to_document(candidate: RetrievalCandidate) -> Document:
        """将统一候选模型转换为 LangChain Document。"""
        return Document(
            page_content=candidate.content,
            metadata={
                "chunk_id": candidate.chunk_id,
                "document_id": candidate.document_id,
                "knowledge_base_id": candidate.knowledge_base_id,
                "chunk_index": candidate.chunk_index,
                "document_name": candidate.document_name,
                "score": candidate.fusion_score,
                "fusion_score": candidate.fusion_score,
                "vector_score": candidate.vector_score,
                "keyword_score": candidate.keyword_score,
                "vector_rank": candidate.vector_rank,
                "keyword_rank": candidate.keyword_rank,
                "retrieval_sources": candidate.retrieval_sources,
                "metadata": candidate.metadata,
            },
        )