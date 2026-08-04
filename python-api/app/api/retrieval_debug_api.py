"""检索调试 API。"""

from functools import lru_cache

from fastapi import APIRouter, Depends

from app.core.response import ApiResult
from app.schemas.retrieval_debug_schema import (
    RetrievalDebugData,
    RetrievalDebugRequest,
)
from app.services.retrieval_debug_service import (
    RetrievalDebugService,
)


router = APIRouter(
    prefix="/api/retrieval",
    tags=["retrieval-debug"],
)


@lru_cache
def get_retrieval_debug_service() -> RetrievalDebugService:
    """创建并缓存检索调试 Service。

    类似 Spring 中默认的单例 Service Bean。
    检索器、模型客户端和数据库配置不需要每次请求重新创建。
    """
    return RetrievalDebugService()


@router.post(
    "/debug",
    response_model=ApiResult[RetrievalDebugData],
    response_model_by_alias=True,
)
def debug_retrieval(
    request: RetrievalDebugRequest,
    service: RetrievalDebugService = Depends(
        get_retrieval_debug_service
    ),
) -> ApiResult[RetrievalDebugData]:
    """执行检索调试，但不生成最终回答。

    执行过程：

    Query Rewrite
        -> Vector / Keyword Search
        -> RRF
        -> Rerank
        -> Context Packing
    """
    data = service.debug(request)

    return ApiResult.ok(data)