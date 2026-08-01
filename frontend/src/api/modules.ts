import { http, parseJsonSafely } from './http';
import { useAuthStore } from '../stores/authStore';
import axios from 'axios';
import type {
  ApiResult,
  ChatConversation,
  ChatMessage,
  ChatRequest,
  CurrentUser,
  DocumentChunk,
  IngestionTask,
  IngestionTaskStep,
  KnowledgeBase,
  KnowledgeDocument,
  PageResult,
  TokenResponse,
} from '../types/api';
export const authApi = {
  login: (v: { tenantCode: string; username: string; password: string }) =>
    http.post<never, TokenResponse>('/api/auth/login', v),
  me: () => http.get<never, CurrentUser>('/api/auth/me'),
  logout: (refreshToken: string) => http.post('/api/auth/logout', { refreshToken }),
};
export const kbApi = {
  page: (p: { keyword?: string; pageNo: number; pageSize: number }) =>
    http.get<never, PageResult<KnowledgeBase>>('/api/knowledge-bases', { params: p }),
  get: (id: string) => http.get<never, KnowledgeBase>(`/api/knowledge-bases/${id}`),
  create: (v: Pick<KnowledgeBase, 'name' | 'description' | 'visibility'>) =>
    http.post<never, KnowledgeBase>('/api/knowledge-bases', v),
  update: (id: string, v: Partial<KnowledgeBase>) =>
    http.patch<never, KnowledgeBase>(`/api/knowledge-bases/${id}`, v),
  remove: (id: string) => http.delete(`/api/knowledge-bases/${id}`),
};
export const documentApi = {
  list: (kbId: string) =>
    http.get<never, KnowledgeDocument[]>(`/api/documents/by-knowledge-base/${kbId}`),
  get: (id: string) => http.get<never, KnowledgeDocument>(`/api/documents/${id}`),
  chunks: (id: string) => http.get<never, DocumentChunk[]>(`/api/documents/${id}/chunks`),
  remove: (id: string) => http.delete(`/api/documents/${id}`),
  // 根据文档 ID 触发后端完整入库流程：解析、切分、Chunk 入库和向量化。
  process: (id: string) =>
    http.post<never, void>(`/api/ingestion/tasks/documents/${id}/process`, undefined, {
      // 文档解析和模型调用属于长耗时操作，单独放宽为 10 分钟。
      timeout: 10 * 60 * 1000,
    }),
  upload: (kbId: string, file: File, onProgress: (n: number) => void) => {
    const data = new FormData();
    data.append('knowledgeBaseId', kbId);
    data.append('file', file);
    return http.post<never, KnowledgeDocument>('/api/documents/upload', data, {
      onUploadProgress: (e) => onProgress(e.total ? Math.round((e.loaded / e.total) * 100) : 0),
    });
  },
};
export const taskApi = {
  get: (id: string) => http.get<never, IngestionTask>(`/api/ingestion/tasks/${id}`),
  steps: (id: string) => http.get<never, IngestionTaskStep[]>(`/api/ingestion/tasks/${id}/steps`),
  getByDocument: (docId: string) =>
    http.get<never, IngestionTask>(`/api/ingestion/tasks/by-document/${docId}`),
  retry: (id: string) => http.post(`/api/ingestion/tasks/${id}/retry`),
  /** @deprecated 上传后自动异步执行，无需手动调用 */
  process: (id: string) => http.post(`/api/ingestion/tasks/${id}/process`),
  /** @deprecated 使用 retry 重试失败任务 */
  embed: (id: string) => http.post(`/api/ingestion/tasks/${id}/embedding`),
};
export const chatApi = {
  conversations: (p: { keyword?: string; pageNo: number; pageSize: number }) =>
    http.get<never, PageResult<ChatConversation>>('/api/chat/conversations', { params: p }),
  messages: (id: string) =>
    http.get<never, ChatMessage[]>(`/api/chat/conversations/${id}/messages`),
  remove: (id: string) => http.delete(`/api/chat/conversations/${id}`),
};
export const streamChat = async (
  request: ChatRequest,
  onEvent: (event: string, data: unknown) => void,
  signal: AbortSignal,
) => {
  const doFetch = async (token: string | null): Promise<Response> => {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = `Bearer ${token}`;
    return fetch(`${http.defaults.baseURL}/api/chat/stream`, {
      method: 'POST',
      headers,
      body: JSON.stringify(request),
      signal,
    });
  };

  let token = useAuthStore.getState().accessToken;
  let response = await doFetch(token);

  // 401 → 尝试刷新 token 后重试一次
  if (response.status === 401 && useAuthStore.getState().refreshToken) {
    try {
      const refreshResponse = await axios.post<ApiResult<TokenResponse>>(
        `${http.defaults.baseURL}/api/auth/refresh`,
        { refreshToken: useAuthStore.getState().refreshToken },
        { transformResponse: [(data: string) => parseJsonSafely(data)] },
      );
      const newSession = refreshResponse.data.data;
      useAuthStore.getState().setSession(newSession);
      token = newSession.accessToken;
      response = await doFetch(token);
    } catch {
      useAuthStore.getState().clear();
      window.location.assign('/login');
      throw new Error('登录已过期，请重新登录');
    }
  }

  if (!response.ok || !response.body) {
    if (response.status === 401) {
      useAuthStore.getState().clear();
      window.location.assign('/login');
      throw new Error('登录已过期');
    }
    throw new Error(`流式请求失败：${response.status}`);
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split('\n\n');
    buffer = blocks.pop() ?? '';
    for (const block of blocks) {
      let event = 'message',
        data = '';
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim();
        if (line.startsWith('data:')) data += line.slice(5).trim();
      }
      if (data) {
        try {
          onEvent(event, parseJsonSafely(data));
        } catch {
          onEvent(event, data);
        }
      }
    }
  }
};
