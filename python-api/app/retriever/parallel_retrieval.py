"""向量检索和关键词检索的共享并行执行器。"""

from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from time import perf_counter
from typing import Callable

from app.schemas.retrieval_schema import RetrievalCandidate


@dataclass
class RetrievalBranchResult:
    """单路检索执行结果。"""

    candidates: list[RetrievalCandidate]
    elapsed_millis: int
    error: Exception | None = None


# 多个请求共享线程池，避免每次检索都创建和销毁线程。
_executor = ThreadPoolExecutor(
    max_workers=4,
    thread_name_prefix="rag-retrieval",
)


def run_parallel_retrieval(
    vector_call: Callable[[], list[RetrievalCandidate]],
    keyword_call: Callable[[], list[RetrievalCandidate]],
) -> tuple[RetrievalBranchResult, RetrievalBranchResult]:
    """并行执行向量检索和关键词检索。"""
    vector_future = _executor.submit(
        _execute_safely,
        vector_call,
    )
    keyword_future = _executor.submit(
        _execute_safely,
        keyword_call,
    )

    # 两个任务提交后已经并行运行，按顺序取结果不会变回串行。
    return vector_future.result(), keyword_future.result()


def _execute_safely(
    retrieval_call: Callable[[], list[RetrievalCandidate]],
) -> RetrievalBranchResult:
    """捕获单路异常并记录准确耗时。"""
    started = perf_counter()

    try:
        candidates = retrieval_call()

        return RetrievalBranchResult(
            candidates=candidates,
            elapsed_millis=_elapsed_millis(started),
        )
    except Exception as exception:
        return RetrievalBranchResult(
            candidates=[],
            elapsed_millis=_elapsed_millis(started),
            error=exception,
        )


def shutdown_retrieval_executor() -> None:
    """应用停止时关闭检索线程池。"""
    _executor.shutdown(
        wait=True,
        cancel_futures=True,
    )


def _elapsed_millis(started: float) -> int:
    return round((perf_counter() - started) * 1000)