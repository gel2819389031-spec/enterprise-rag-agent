export type EvaluationExperiment =
  | 'VECTOR'
  | 'KEYWORD'
  | 'HYBRID'
  | 'HYBRID_RERANK'
  | 'HYBRID_REWRITE'
  | 'HYBRID_REWRITE_RERANK';
export type EvaluationStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

export interface EvaluationCreateRequest {
  knowledgeBaseId: string;
  datasetCode: 'CRUD_RAG_V1' | 'CRUD_RAG_V2';
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
  evidenceCaseCount: number;
  chunkHitAt5: number | null;
  averageLatencyMillis: number;
  p95LatencyMillis: number;
}

/** 单个召回分块，用于核对不同检索实验是否真的返回相同结果。 */
export interface EvaluationCandidate {
  documentId: string;
  documentName: string | null;
  chunkId: string;
  chunkIndex: number;
  vectorScore: number | null;
  keywordScore: number | null;
  fusionScore: number | null;
  rerankScore: number | null;
  content: string;
}

export interface EvaluationDetail {
  caseId: string;
  experiment: EvaluationExperiment;
  question: string;
  goldDocumentNames: string[];
  retrievedDocumentNames: string[];
  retrievedCandidates: EvaluationCandidate[];
  semanticQuery: string;
  keywords: string[];
  rewriteApplied: boolean;
  rerankApplied: boolean;
  firstRelevantRank: number | null;
  evidenceEvaluated: boolean;
  firstRelevantChunkRank: number | null;
  chunkHitAt5: boolean | null;
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
