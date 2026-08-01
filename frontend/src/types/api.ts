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
export type DocumentParseStatus = 'PENDING' | 'PARSING' | 'PARSED' | 'READY' | 'FAILED';
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
export interface IngestionTaskStep {
  id: string;
  taskId: string;
  stepName: string;
  status: IngestionStatus;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
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
