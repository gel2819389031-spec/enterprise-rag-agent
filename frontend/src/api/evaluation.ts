import { http } from './http';
import type {
  EvaluationCreateRequest,
  EvaluationResult,
  EvaluationRun,
} from '../types/evaluation';

/** RAG 检索测评接口。 */
export const evaluationApi = {
  create: (request: EvaluationCreateRequest) =>
    http.post<never, EvaluationRun>('/api/evaluations/retrieval', request),
  getStatus: (runId: string) =>
    http.get<never, EvaluationRun>(`/api/evaluations/retrieval/${runId}`),
  getResult: (runId: string) =>
    http.get<never, EvaluationResult>(`/api/evaluations/retrieval/${runId}/result`),
};
