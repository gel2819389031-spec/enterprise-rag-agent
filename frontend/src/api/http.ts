import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import JSONBigFactory from 'json-bigint';
import { useAuthStore } from '../stores/authStore';
import type { ApiResult, TokenResponse } from '../types/api';

/** 前端统一接口异常。 */
export class ApiError extends Error {
  constructor(
    public code: string,
    message: string,
    public status?: number,
  ) {
    super(message);
  }
}

// 后端 Long 可能超过 Number.MAX_SAFE_INTEGER，解析时统一保留为字符串。
const JSONBig = JSONBigFactory({ storeAsString: true });

export const parseJsonSafely = (value: string): unknown => {
  if (!value) return value;
  try {
    return JSONBig.parse(value) as unknown;
  } catch {
    return value;
  }
};

export const http = axios.create({
  // 开发环境通过 Vite proxy 转发 /api → localhost:8123，无需设置此变量。
  // 需要直连后端时设置 VITE_API_BASE_URL=http://127.0.0.1:8123
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 30000,
  transformResponse: [(data: string) => parseJsonSafely(data)],
});

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshing: Promise<string> | null = null;

const refresh = async () => {
  const state = useAuthStore.getState();
  if (!state.refreshToken) throw new Error('No refresh token');

  const response = await axios.post<ApiResult<TokenResponse>>(
    `${http.defaults.baseURL}/api/auth/refresh`,
    { refreshToken: state.refreshToken },
    { transformResponse: [(data: string) => parseJsonSafely(data)] },
  );
  state.setSession(response.data.data);
  return response.data.data.accessToken;
};

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>;
    if (body && typeof body === 'object' && 'success' in body) {
      if (!body.success) throw new ApiError(body.code, body.message, response.status);
      return body.data;
    }
    return response.data;
  },
  async (error: AxiosError<ApiResult<unknown>>) => {
    const original = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined;

    if (
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      !original.url?.includes('/api/auth/')
    ) {
      original._retry = true;
      try {
        refreshing ??= refresh().finally(() => {
          refreshing = null;
        });
        original.headers.Authorization = `Bearer ${await refreshing}`;
        return http(original);
      } catch {
        useAuthStore.getState().clear();
        window.location.assign('/login');
        return Promise.reject(error);
      }
    }

    const body = error.response?.data;
    throw new ApiError(
      body?.code ?? `HTTP_${error.response?.status ?? 0}`,
      body?.message ?? error.message,
      error.response?.status,
    );
  },
);
