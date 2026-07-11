import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

/**
 * 后台 API 客户端（明文 JSON + Bearer）
 * - 统一挂 Authorization: Bearer
 * - 401（access 过期）尝试用 refreshToken 静默刷新并原样重试一次
 * - 401（refresh 也失败）/ 会话异常 -> 触发 onSessionExpired 回调（跳登录）
 * - 403 -> 透传给业务层 toast（无写权限 / 非 super）
 */
// API 根地址：优先读取构建期环境变量 VITE_API_BASE，未配置时回退为空串（同源请求）。
const API_BASE: string = (import.meta.env.VITE_API_BASE as string) || '';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// 内存态令牌：仅在运行时有效，刷新页面后由 auth store 从 localStorage 重新注入。
let accessToken: string | null = null;
let refreshToken: string | null = null;

// 全局会话回调（由业务层注入）：会话失效 / 令牌刷新成功时统一通知上层。
let onSessionExpired: (() => void) | null = null;
let onTokensRefreshed: ((access: string, refresh: string) => void) | null = null;

/**
 * 写入（或清空）内存态令牌，并同步到 axios 实例的默认 Authorization 头。
 * @param access 新的 access token；传 null 表示登出/清理。
 * @param refresh 新的 refresh token。
 * @sideeffect 修改模块级 accessToken/refreshToken，以及 api.defaults.headers.common。
 */
export function setTokens(access: string | null, refresh: string | null): void {
  accessToken = access;
  refreshToken = refresh;
  if (access) api.defaults.headers.common['Authorization'] = `Bearer ${access}`;
  else delete api.defaults.headers.common['Authorization'];
}

/**
 * 读取当前内存态 refresh token（供静默刷新请求体使用）。
 * @returns 当前 refreshToken，未登录时为 null。
 */
export function getRefreshToken(): string | null {
  return refreshToken;
}

/**
 * 注册「会话失效」回调（通常由 App 根组件注入，用于跳转登录页）。
 * @param fn 无参无返回值，触发时机：refresh 失败或无 refreshToken。
 */
export function setSessionExpiredHandler(fn: () => void): void {
  onSessionExpired = fn;
}

/**
 * 注册「令牌刷新成功」回调（通常由 auth store 注入，用于同步持久化态）。
 * @param fn 参数为刷新后的 access 与 refresh token。
 */
export function setTokensRefreshedHandler(fn: (access: string, refresh: string) => void): void {
  onTokensRefreshed = fn;
}

// 请求拦截器：在发出前统一注入最新的 access token（避免依赖 axios 默认头同步时机）。
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

// 并发刷新控制：isRefreshing 标记是否已有刷新在途；pendingQueue 缓存等待中的请求回调。
let isRefreshing = false;
let pendingQueue: Array<(token: string | null) => void> = [];

// 响应拦截器：仅处理 401 静默刷新；其余错误直接 reject 透传给业务层。
api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

    // 登录/刷新接口本身不触发刷新逻辑，避免无限递归刷新。
    const isAuthEndpoint =
      !!original?.url && (original.url.includes('/api/admin/login') || original.url.includes('/api/admin/refresh'));

    if (error.response?.status === 401 && original && !original._retry && !isAuthEndpoint) {
      original._retry = true;

      if (!refreshToken) {
        onSessionExpired?.();
        return Promise.reject(error);
      }

      // 首个 401：由当前请求独占发起刷新，期间其它 401 进入排队。
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
          // 刷新失败（网络/refresh 失效）：释放队列并通知会话失效。
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
