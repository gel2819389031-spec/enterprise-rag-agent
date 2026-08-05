export type EvaluationExperiment = 'VECTOR' | 'KEYWORD' | 'HYBRID' | 'HYBRID_RERANK';
export type EvaluationStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

export interface EvaluationCreateRequest {
  knowledgeBaseId: string;
  datasetCode: 'CRUD_RAG_V1';
  experiments: EvaluationExperiment[];
}

export interface EvaluationRun {
  runId: string;
  status: EvaluationStatus;
  datasetCode: string;
  knowledgeBaseId: string;
  totalCases: number;
  completedCases: number;
  progress: number;
  currentExperiment: EvaluationExperiment | null;
  errorMessage: string | null;
  createdAt: string;
  finishedAt: string | null;
}

export interface EvaluationSummary {
  experiment: EvaluationExperiment;
  caseCount: number;
  failedCount: number;
  degradedCount: number;
  hitAt1: number;
  hitAt3: number;
  hitAt5: number;
  hitAt8: number;
  mrr: number;
  averageLatencyMillis: number;
  p95LatencyMillis: number;
}

export interface EvaluationDetail {
  caseId: string;
  experiment: EvaluationExperiment;
  question: string;
  goldDocumentNames: string[];
  retrievedDocumentNames: string[];
  firstRelevantRank: number | null;
  hitAt5: boolean;
  latencyMillis: number;
  degraded: boolean;
  error: string | null;
}

export interface EvaluationResult {
  runId: string;
  status: EvaluationStatus;
  summaries: EvaluationSummary[];
  details: EvaluationDetail[];
}
