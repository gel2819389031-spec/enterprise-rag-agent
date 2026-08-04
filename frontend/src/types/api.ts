export interface ApiResult<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}
export interface PageResult<T> {
  records: T[];
  total: number;
  pageNo: number;
  pageSize: number;
  pages: number;
  hasNext: boolean;
}
export interface TokenResponse {
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  userId: string;
  tenantId: string;
  username: string;
  displayName: string;
  role: string;
}
export interface CurrentUser {
  userId: string;
  tenantId: string;
  username: string;
  displayName: string;
  email?: string;
  roleCode: string;
}

// --- 枚举类型 ---

export type KnowledgeBaseVisibility = 'PRIVATE' | 'TENANT' | 'PUBLIC';
export type DocumentParseStatus =
  | 'PENDING'     // 排队中
  | 'PROCESSING'  // 解析切分中
  | 'PARSED'      // 已解析，等待向量化
  | 'EMBEDDING'   // 向量化中
  | 'READY'       // 处理完成
  | 'FAILED';     // 处理失败
export type IngestionTaskType = 'DOCUMENT_INGEST' | 'EMBEDDING';
export type IngestionStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELED';
export type ChatChannel = 'WEB' | 'API';
export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM';

// --- 实体类型 ---

export interface KnowledgeBase {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  visibility: KnowledgeBaseVisibility;
  embeddingModelConfigId?: string;
  chunkStrategy?: string;
  status: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  deleted: boolean;
}
export interface KnowledgeDocument {
  id: string;
  tenantId: string;
  knowledgeBaseId: string;
  fileName: string;
  fileType: string;
  fileUri: string;
  fileSize: number;
  contentHash: string;
  parseStatus: DocumentParseStatus;
  metadata: Record<string, unknown>;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  deleted: boolean;
}
export interface DocumentChunk {
  id: string;
  documentId: string;
  chunkIndex: number;
  content: string;
  tokenCount?: number;
  embeddingModel?: string;
  metadata: Record<string, unknown>;
  createdAt: string;
}
export interface IngestionTask {
  id: string;
  tenantId: string;
  knowledgeBaseId: string;
  documentId: string;
  taskType: IngestionTaskType;
  status: IngestionStatus;
  errorMessage?: string;
  progress: number;
  startedAt?: string;
  finishedAt?: string;
  createdAt: string;
  updatedAt: string;
}

/** 任务中心分页查询条件，与后端 IngestionTaskQueryRequest 对齐。 */
export interface IngestionTaskQuery {
  keyword?: string;
  status?: IngestionStatus;
  taskType?: IngestionTaskType;
  knowledgeBaseId?: string;
  documentId?: string;
  createdBy?: string;
  createdAtStart?: string;
  createdAtEnd?: string;
  pageNo: number;
  pageSize: number;
}

/** 任务中心列表项，与后端 IngestionTaskListResponse 对齐。 */
export interface IngestionTaskListItem extends IngestionTask {
  knowledgeBaseName?: string;
  documentName?: string;
  currentStepCode?: string;
  currentStepName?: string;
  durationMillis?: number;
  createdBy?: string;
}

/** 任务详情，与后端 IngestionTaskDetailResponse 对齐。 */
export interface IngestionTaskDetail extends IngestionTaskListItem {
  fileType?: string;
  fileSize?: number;
  documentStatus?: DocumentParseStatus;
  canRetry: boolean;
  steps: IngestionTaskStep[];
  knowledgeBaseDeleted?: boolean;
  documentDeleted?: boolean;
}

/** 任务统计，与后端 IngestionTaskStatisticsResponse 对齐。 */
export interface IngestionTaskStatistics {
  totalCount: number;
  pendingCount: number;
  runningCount: number;
  successCount: number;
  failedCount: number;
  successRate: number;
  averageDurationMillis: number;
  todayCreatedCount: number;
  todaySuccessCount: number;
  todayFailedCount: number;
}

/** 任务统计查询条件，与后端 IngestionTaskStatisticsQuery 对齐。 */
export interface IngestionTaskStatisticsQuery {
  knowledgeBaseId?: string;
  createdAtStart?: string;
  createdAtEnd?: string;
}

export interface IngestionTaskStep {
  id: string;
  taskId: string;
  stepCode: string;
  stepName: string;
  status: IngestionStatus;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
  durationMillis?: number;
}
export interface ChatConversation {
  id: string;
  tenantId: string;
  userId: string;
  knowledgeBaseId?: string;
  title: string;
  channel: ChatChannel;
  createdAt: string;
  updatedAt: string;
}
export interface ChatMessage {
  id: string;
  conversationId: string;
  role: MessageRole;
  content: string;
  citations?: unknown[];
  traceId?: string;
  createdAt: string;
}
export interface ChatRequest {
  conversationId?: string;
  knowledgeBaseId?: string;
  question: string;
  model?: string;
}
