"""Reciprocal Rank Fusion 排名融合。"""

from app.schemas.retrieval_schema import RetrievalCandidate


class RrfFusion:
    """根据各路检索排名融合候选分片。"""

    def __init__(self, rrf_k: int = 60) -> None:
        """设置 RRF 平滑参数。"""
        if rrf_k <= 0:
            raise ValueError("rrf_k must be greater than 0")

        self._rrf_k = rrf_k

    def fuse(
        self,
        vector_candidates: list[RetrievalCandidate],
        keyword_candidates: list[RetrievalCandidate],
        final_top_k: int,
    ) -> list[RetrievalCandidate]:
        """按 chunk_id 去重并计算 RRF 分数。"""
        if final_top_k <= 0:
            return []

        merged: dict[int, RetrievalCandidate] = {}

        # 合并向量检索候选项。
        for rank, candidate in enumerate(vector_candidates, start=1):
            item = candidate.model_copy(deep=True)

            # 统一使用实际列表位置作为排名。
            item.vector_rank = rank
            item.fusion_score = self._rrf_score(rank)

            if "vector" not in item.retrieval_sources:
                item.retrieval_sources.append("vector")

            merged[item.chunk_id] = item

        # 合并关键词检索候选项。
        for rank, candidate in enumerate(keyword_candidates, start=1):
            existing = merged.get(candidate.chunk_id)

            if existing is None:
                item = candidate.model_copy(deep=True)
                item.keyword_rank = rank
                item.fusion_score = self._rrf_score(rank)

                if "keyword" not in item.retrieval_sources:
                    item.retrieval_sources.append("keyword")

                merged[item.chunk_id] = item
                continue

            # 同一分片被两路召回时合并排名和分数。
            existing.keyword_rank = rank
            existing.keyword_score = candidate.keyword_score
            existing.fusion_score += self._rrf_score(rank)

            if "keyword" not in existing.retrieval_sources:
                existing.retrieval_sources.append("keyword")

        # RRF 分数相同时，优先选择向量相似度较高的结果。
        sorted_candidates = sorted(
            merged.values(),
            key=lambda item: (
                item.fusion_score,
                item.vector_score or 0.0,
                item.keyword_score or 0.0,
            ),
            reverse=True,
        )

        return sorted_candidates[:final_top_k]

    def _rrf_score(self, rank: int) -> float:
        """计算单路检索贡献的 RRF 分数。"""
        return 1.0 / (self._rrf_k + rank)