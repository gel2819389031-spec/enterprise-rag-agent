"""异步执行 CRUD-RAG 检索测评。"""

import json
import math
import threading
import uuid
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from time import perf_counter
from typing import Any

from app.evaluation.models import CorpusDocument, EvaluationCase
from app.schemas.evaluation_schema import (
    EvaluationCreateRequest,
    EvaluationExperiment,
    EvaluationResultData,
    EvaluationRunData,
)
from app.schemas.retrieval_debug_schema import RetrievalDebugRequest, RetrievalMode
from app.services.retrieval_debug_service import RetrievalDebugService


EXPERIMENT_CONFIG = {
    EvaluationExperiment.VECTOR: (RetrievalMode.VECTOR, False, "vector_results"),
    EvaluationExperiment.KEYWORD: (RetrievalMode.KEYWORD, False, "keyword_results"),
    EvaluationExperiment.HYBRID: (RetrievalMode.HYBRID, False, "fusion_results"),
    EvaluationExperiment.HYBRID_RERANK: (RetrievalMode.HYBRID, True, "rerank_results"),
}


@dataclass
class EvaluationJob:
    """进程内评测任务状态。"""

    run_id: str
    request: EvaluationCreateRequest
    status: str = "PENDING"
    total_cases: int = 0
    completed_cases: int = 0
    current_experiment: str | None = None
    error_message: str | None = None
    created_at: str = field(default_factory=lambda: _now())
    finished_at: str | None = None
    summaries: list[dict[str, Any]] = field(default_factory=list)
    details: list[dict[str, Any]] = field(default_factory=list)


class EvaluationService:
    """管理并执行单进程异步评测任务。"""

    def __init__(self) -> None:
        # 第一版串行执行任务，避免评测流量压满模型服务。
        self._executor = ThreadPoolExecutor(
            max_workers=1,
            thread_name_prefix="rag-evaluation",
        )
        self._jobs: dict[str, EvaluationJob] = {}
        self._lock = threading.Lock()
        self._python_root = Path(__file__).resolve().parents[2]

    def create(self, request: EvaluationCreateRequest) -> EvaluationRunData:
        """登记评测任务并提交后台线程。"""
        dataset_dir = self._resolve_dataset(request.dataset_code)
        cases = self._load_cases(dataset_dir)
        run_id = uuid.uuid4().hex
        job = EvaluationJob(
            run_id=run_id,
            request=request,
            total_cases=len(cases) * len(request.experiments),
        )

        with self._lock:
            self._jobs[run_id] = job

        self._executor.submit(self._execute, run_id, dataset_dir, cases)
        return self._to_run_data(job)

    def get_status(self, run_id: str, tenant_id: int, user_id: int) -> EvaluationRunData:
        """查询当前用户创建的任务状态。"""
        return self._to_run_data(self._require_job(run_id, tenant_id, user_id))

    def get_result(self, run_id: str, tenant_id: int, user_id: int) -> EvaluationResultData:
        """查询任务结果。"""
        job = self._require_job(run_id, tenant_id, user_id)
        return EvaluationResultData(
            run_id=job.run_id,
            status=job.status,
            summaries=list(job.summaries),
            details=list(job.details),
        )

    def _execute(
        self,
        run_id: str,
        dataset_dir: Path,
        cases: list[EvaluationCase],
    ) -> None:
        """后台执行全部实验。"""
        job = self._jobs[run_id]
        try:
            mapping = self._load_document_mapping(dataset_dir)
            service = RetrievalDebugService()
            self._update_job(job, status="RUNNING")

            for experiment in job.request.experiments:
                self._update_job(job, current_experiment=experiment.value)
                experiment_details: list[dict[str, Any]] = []

                for case in cases:
                    detail = self._evaluate_case(
                        service=service,
                        request=job.request,
                        experiment=experiment,
                        case=case,
                        document_mapping=mapping,
                    )
                    experiment_details.append(detail)
                    with self._lock:
                        job.details.append(detail)
                        job.completed_cases += 1

                summary = self._summarize(experiment.value, experiment_details)
                with self._lock:
                    job.summaries.append(summary)

            self._update_job(
                job,
                status="SUCCESS",
                current_experiment=None,
                finished_at=_now(),
            )
        except Exception as exception:
            self._update_job(
                job,
                status="FAILED",
                error_message=str(exception),
                finished_at=_now(),
            )

    def _evaluate_case(
        self,
        service: RetrievalDebugService,
        request: EvaluationCreateRequest,
        experiment: EvaluationExperiment,
        case: EvaluationCase,
        document_mapping: dict[str, str],
    ) -> dict[str, Any]:
        """执行一条 Case，失败时按未命中计分。"""
        mode, enable_rerank, result_field = EXPERIMENT_CONFIG[experiment]
        gold_names = {document_mapping[key] for key in case.gold_document_keys}
        started = perf_counter()

        try:
            data = service.debug(
                RetrievalDebugRequest(
                    tenant_id=request.tenant_id,
                    user_id=request.user_id,
                    knowledge_base_id=request.knowledge_base_id,
                    question=case.question,
                    mode=mode,
                    enable_rewrite=True,
                    enable_rerank=enable_rerank,
                    vector_top_k=30,
                    keyword_top_k=30,
                    fusion_top_k=20,
                    final_top_k=8,
                    rrf_k=60,
                )
            )
            candidates = getattr(data, result_field)
            names = [item.document_name for item in candidates if item.document_name]
            first_rank = next(
                (rank for rank, name in enumerate(names, start=1) if name in gold_names),
                None,
            )
            return self._detail(
                case=case,
                experiment=experiment.value,
                gold_names=gold_names,
                first_rank=first_rank,
                latency=self._elapsed(started),
                degraded=data.degraded,
                error=None,
                retrieved_names=names[:8],
            )
        except Exception as exception:
            return self._detail(
                case=case,
                experiment=experiment.value,
                gold_names=gold_names,
                first_rank=None,
                latency=self._elapsed(started),
                degraded=False,
                error=str(exception),
                retrieved_names=[],
            )

    @staticmethod
    def _detail(
        case: EvaluationCase,
        experiment: str,
        gold_names: set[str],
        first_rank: int | None,
        latency: int,
        degraded: bool,
        error: str | None,
        retrieved_names: list[str],
    ) -> dict[str, Any]:
        return {
            "caseId": case.case_id,
            "experiment": experiment,
            "question": case.question,
            "goldDocumentNames": sorted(gold_names),
            "retrievedDocumentNames": retrieved_names,
            "firstRelevantRank": first_rank,
            "hitAt1": first_rank is not None and first_rank <= 1,
            "hitAt3": first_rank is not None and first_rank <= 3,
            "hitAt5": first_rank is not None and first_rank <= 5,
            "hitAt8": first_rank is not None and first_rank <= 8,
            "reciprocalRank": 1.0 / first_rank if first_rank else 0.0,
            "latencyMillis": latency,
            "degraded": degraded,
            "error": error,
        }

    @staticmethod
    def _summarize(experiment: str, details: list[dict[str, Any]]) -> dict[str, Any]:
        total = len(details)
        latencies = sorted(item["latencyMillis"] for item in details)
        p95_index = max(0, math.ceil(len(latencies) * 0.95) - 1)

        def rate(field_name: str) -> float:
            return sum(bool(item[field_name]) for item in details) / total if total else 0.0

        return {
            "experiment": experiment,
            "caseCount": total,
            "failedCount": sum(item["error"] is not None for item in details),
            "degradedCount": sum(bool(item["degraded"]) for item in details),
            "hitAt1": rate("hitAt1"),
            "hitAt3": rate("hitAt3"),
            "hitAt5": rate("hitAt5"),
            "hitAt8": rate("hitAt8"),
            "mrr": sum(item["reciprocalRank"] for item in details) / total if total else 0.0,
            "averageLatencyMillis": sum(latencies) / len(latencies) if latencies else 0.0,
            "p95LatencyMillis": latencies[p95_index] if latencies else 0,
        }

    def _resolve_dataset(self, dataset_code: str) -> Path:
        datasets = {
            "CRUD_RAG_V1": self._python_root / "evaluation" / "datasets" / "crud_v1"
        }
        path = datasets.get(dataset_code)
        if path is None or not path.is_dir():
            raise ValueError(f"Unsupported or missing dataset: {dataset_code}")
        return path

    @staticmethod
    def _load_cases(dataset_dir: Path) -> list[EvaluationCase]:
        return [
            EvaluationCase.model_validate(item)
            for item in EvaluationService._read_jsonl(dataset_dir / "cases.jsonl")
        ]

    @staticmethod
    def _load_document_mapping(dataset_dir: Path) -> dict[str, str]:
        documents = [
            CorpusDocument.model_validate(item)
            for item in EvaluationService._read_jsonl(dataset_dir / "corpus-manifest.jsonl")
        ]
        return {item.document_key: item.file_name for item in documents}

    @staticmethod
    def _read_jsonl(path: Path) -> list[dict[str, Any]]:
        with path.open("r", encoding="utf-8") as file:
            return [json.loads(line) for line in file if line.strip()]

    def _require_job(self, run_id: str, tenant_id: int, user_id: int) -> EvaluationJob:
        with self._lock:
            job = self._jobs.get(run_id)
            if job is None:
                raise KeyError("Evaluation run not found")
            if job.request.tenant_id != tenant_id or job.request.user_id != user_id:
                raise PermissionError("Evaluation run access denied")
            return job

    def _to_run_data(self, job: EvaluationJob) -> EvaluationRunData:
        with self._lock:
            progress = (
                round(job.completed_cases * 100 / job.total_cases)
                if job.total_cases
                else 0
            )
            return EvaluationRunData(
                run_id=job.run_id,
                status=job.status,
                dataset_code=job.request.dataset_code,
                knowledge_base_id=job.request.knowledge_base_id,
                total_cases=job.total_cases,
                completed_cases=job.completed_cases,
                progress=progress,
                current_experiment=job.current_experiment,
                error_message=job.error_message,
                created_at=job.created_at,
                finished_at=job.finished_at,
            )

    def _update_job(self, job: EvaluationJob, **values: Any) -> None:
        with self._lock:
            for name, value in values.items():
                setattr(job, name, value)

    @staticmethod
    def _elapsed(started: float) -> int:
        return round((perf_counter() - started) * 1000)


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()
