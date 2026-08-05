"""向量检索、关键词检索和 RRF 融合的统一入口。"""

import logging

from langchain_core.documents import Document

from app.config import get_settings
from app.retriever.keyword_retriever import KeywordRetriever
from app.retriever.parallel_retrieval import (
    run_parallel_retrieval,
)
from app.retriever.pgvector_retriever import PgVectorRetriever
from app.retriever.rrf_fusion import RrfFusion
from app.schemas.retrieval_schema import RetrievalCandidate


logger = logging.getLogger(__name__)


class HybridRetriever:
    """并行执行两路检索，然后使用 RRF 融合结果。"""

    def __init__(self) -> None:
        """初始化检索器和融合器。"""
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
        """并行执行向量和关键词检索并返回融合结果。"""

        # 将两路检索同时提交到共享线程池。
        vector_result, keyword_result = run_parallel_retrieval(
            vector_call=lambda: self._vector_retriever.retrieve(
                question=semantic_query,
                tenant_id=tenant_id,
                knowledge_base_id=knowledge_base_id,
                top_k=self._settings.retrieval_vector_top_k,
            ),
            keyword_call=lambda: self._retrieve_by_keywords(
                keywords=keywords,
                tenant_id=tenant_id,
                knowledge_base_id=knowledge_base_id,
            ),
        )

        # 分别获取候选结果和异常。
        vector_candidates = vector_result.candidates
        keyword_candidates = keyword_result.candidates
        vector_error = vector_result.error
        keyword_error = keyword_result.error

        # 记录向量检索异常。
        if vector_error is not None:
            self._log_retrieval_error(
                message=(
                    "向量检索失败, tenant_id=%s, "
                    "knowledge_base_id=%s"
                ),
                error=vector_error,
                tenant_id=tenant_id,
                knowledge_base_id=knowledge_base_id,
            )

        # 记录关键词检索异常。
        if keyword_error is not None:
            self._log_retrieval_error(
                message=(
                    "关键词检索失败, tenant_id=%s, "
                    "knowledge_base_id=%s"
                ),
                error=keyword_error,
                tenant_id=tenant_id,
                knowledge_base_id=knowledge_base_id,
            )

        # 两路都执行失败时，不能继续生成无依据回答。
        if (
            vector_error is not None
            and keyword_error is not None
        ):
            raise RuntimeError(
                "Vector and keyword retrieval both failed"
            ) from vector_error

        # 关键词检索未启用时，向量检索失败后无法降级。
        if (
            vector_error is not None
            and not self._settings.retrieval_enable_keyword
        ):
            raise RuntimeError(
                "Vector retrieval failed"
            ) from vector_error

        # 没有有效关键词时，向量检索失败也无法降级。
        if vector_error is not None and not keywords:
            raise RuntimeError(
                "Vector retrieval failed and keywords are empty"
            ) from vector_error

        # 一路失败时，RRF 会自动使用另一路已有候选结果。
        fused_candidates = self._rrf_fusion.fuse(
            vector_candidates=vector_candidates,
            keyword_candidates=keyword_candidates,
            final_top_k=self._settings.retrieval_fusion_top_k,
        )

        # 转换为 ContextPacker 和后续 RAG 链路使用的 Document。
        return [
            self._to_document(candidate)
            for candidate in fused_candidates
        ]

    def _retrieve_by_keywords(
        self,
        keywords: list[str],
        tenant_id: int,
        knowledge_base_id: int,
    ) -> list[RetrievalCandidate]:
        """根据配置决定是否执行关键词检索。"""
        if (
            not self._settings.retrieval_enable_keyword
            or not keywords
        ):
            return []

        return self._keyword_retriever.retrieve(
            keywords=keywords,
            tenant_id=tenant_id,
            knowledge_base_id=knowledge_base_id,
            top_k=self._settings.retrieval_keyword_top_k,
        )

    @staticmethod
    def _log_retrieval_error(
        message: str,
        error: Exception,
        tenant_id: int,
        knowledge_base_id: int,
    ) -> None:
        """记录异步线程中产生的完整异常堆栈。"""
        logger.error(
            message,
            tenant_id,
            knowledge_base_id,
            exc_info=(
                type(error),
                error,
                error.__traceback__,
            ),
        )

    @staticmethod
    def _to_document(
        candidate: RetrievalCandidate,
    ) -> Document:
        """将统一候选结果转换为 LangChain Document。"""
        return Document(
            page_content=candidate.content,
            metadata={
                "chunk_id": candidate.chunk_id,
                "document_id": candidate.document_id,
                "knowledge_base_id": (
                    candidate.knowledge_base_id
                ),
                "chunk_index": candidate.chunk_index,
                "document_name": candidate.document_name,
                "score": candidate.fusion_score,
                "fusion_score": candidate.fusion_score,
                "vector_score": candidate.vector_score,
                "keyword_score": candidate.keyword_score,
                "vector_rank": candidate.vector_rank,
                "keyword_rank": candidate.keyword_rank,
                "retrieval_sources": (
                    candidate.retrieval_sources
                ),
                "metadata": candidate.metadata,
            },
        )