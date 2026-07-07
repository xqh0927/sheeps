import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

/**
 * 后台 API 客户端（明文 JSON + Bearer）
 * - 统一挂 Authorization: Bearer
 * - 401（access 过期）尝试用 refreshToken 静默刷新并原样重试一次
 * - 401（refresh 也失败）/ 会话异常 -> 触发 onSessionExpired 回调（跳登录）
 * - 403 -> 透传给业务层 toast（无写权限 / 非 super）
 */
const API_BASE: string = (import.meta.env.VITE_API_BASE as string) || '';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

let accessToken: string | null = null;
let refreshToken: string | null = null;

let onSessionExpired: (() => void) | null = null;
let onTokensRefreshed: ((access: string, refresh: string) => void) | null = null;

export function setTokens(access: string | null, refresh: string | null): void {
  accessToken = access;
  refreshToken = refresh;
  if (access) api.defaults.headers.common['Authorization'] = `Bearer ${access}`;
  else delete api.defaults.headers.common['Authorization'];
}

export function getRefreshToken(): string | null {
  return refreshToken;
}

export function setSessionExpiredHandler(fn: () => void): void {
  onSessionExpired = fn;
}

export function setTokensRefreshedHandler(fn: (access: string, refresh: string) => void): void {
  onTokensRefreshed = fn;
}

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

let isRefreshing = false;
let pendingQueue: Array<(token: string | null) => void> = [];

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

    const isAuthEndpoint =
      !!original?.url && (original.url.includes('/api/admin/login') || original.url.includes('/api/admin/refresh'));

    if (error.response?.status === 401 && original && !original._retry && !isAuthEndpoint) {
      original._retry = true;

      if (!refreshToken) {
        onSessionExpired?.();
        return Promise.reject(error);
      }

      if (!isRefreshing) {
        isRefreshing = true;
        try {
          const resp = await axios.post(`${API_BASE}/api/admin/refresh`, { refreshToken });
          if (resp.data?.success) {
            accessToken = resp.data.token;
            refreshToken = resp.data.refreshToken;
            api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            onTokensRefreshed?.(accessToken!, refreshToken!);
            pendingQueue.forEach((cb) => cb(accessToken));
            pendingQueue = [];
            isRefreshing = false;
            return api(original);
          }
          throw new Error('refresh_failed');
        } catch {
          isRefreshing = false;
          pendingQueue.forEach((cb) => cb(null));
          pendingQueue = [];
          onSessionExpired?.();
          return Promise.reject(error);
        }
      }

      // 已有刷新进行中，排队等待
      return new Promise((resolve, reject) => {
        pendingQueue.push((token) => {
          if (token) {
            original.headers.Authorization = `Bearer ${token}`;
            resolve(api(original));
          } else {
            reject(error);
          }
        });
      });
    }

    return Promise.reject(error);
  }
);

/** 统一解析错误文案：优先后端 {error}，否则 HTTP 状态描述 */
export function extractError(err: unknown): string {
  const e = err as AxiosError<{ error?: string }>;
  if (e?.response?.data?.error) return e.response.data.error;
  if (e?.message) return e.message;
  return '请求失败，请稍后重试';
}
