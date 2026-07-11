import { create } from 'zustand';
import { api, setTokens, setTokensRefreshedHandler } from '../api/client';

/** 后台账号角色：super=超管，operator=运营（可写），readonly=只读，user=普通（无后台权限）。 */
export type AdminRole = 'super' | 'operator' | 'readonly' | 'user';

/** 当前登录的后台用户信息（来自登录接口响应）。 */
export interface AdminUser {
  id: string;
  phone: string;
  username: string;
  role: AdminRole;
}

/**
 * 全局认证状态（Zustand store）。
 * - token/refreshTokenVal：当前会话令牌，与 client 层内存态保持同步；
 * - user：登录后写入，驱动权限判断与界面展示；
 * - login/logout：与后端鉴权流同步，并触发持久化；
 * - isSuper/canWrite：派生权限（基于 user.role）；
 * - initFromStorage：应用启动时从 localStorage 恢复会话。
 */
interface AuthState {
  token: string | null;
  refreshTokenVal: string | null;
  user: AdminUser | null;
  login: (phone: string, password: string) => Promise<AdminUser>;
  logout: () => void;
  isSuper: () => boolean;
  canWrite: () => boolean;
  initFromStorage: () => void;
}

/** localStorage 持久化键名。 */
const STORAGE_KEY = 'miadmin_auth';

/** 持久化结构：与 STORAGE_KEY 中 JSON 对应。 */
interface PersistedAuth {
  token: string;
  refreshToken: string;
  user: AdminUser;
}

/** 写/清 localStorage：三个字段齐全才持久化，否则清除（视为登出）。 */
function persist(token: string | null, refresh: string | null, user: AdminUser | null): void {
  if (token && refresh && user) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, refreshToken: refresh, user }));
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}

export const useAuth = create<AuthState>((set, get) => {
  // 订阅 client 层令牌刷新：刷新成功后同步内存态 + 持久化，保持两端令牌一致。
  setTokensRefreshedHandler((access, refresh) => {
    const user = get().user;
    set({ token: access, refreshTokenVal: refresh });
    persist(access, refresh, user);
  });

  return {
    token: null,
    refreshTokenVal: null,
    user: null,

    initFromStorage: () => {
      // 冷启动恢复：读取持久化会话，注入 client 层并回填 store；解析失败则清理脏数据。
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      try {
        const parsed = JSON.parse(raw) as PersistedAuth;
        setTokens(parsed.token, parsed.refreshToken);
        set({ token: parsed.token, refreshTokenVal: parsed.refreshToken, user: parsed.user });
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
    },

    // 登录：POST /api/admin/login，成功后写入令牌、store 与持久化，并返回用户信息。
    login: async (phone, password) => {
      const resp = await api.post('/api/admin/login', { phone, password });
      const { token, refreshToken, user } = resp.data;
      setTokens(token, refreshToken);
      set({ token, refreshTokenVal: refreshToken, user });
      persist(token, refreshToken, user);
      return user;
    },

    // 登出：清空 client 内存态、store 与持久化，完成会话销毁。
    logout: () => {
      setTokens(null, null);
      set({ token: null, refreshTokenVal: null, user: null });
      persist(null, null, null);
    },

    // 权限派生：是否为超级管理员。
    isSuper: () => get().user?.role === 'super',
    // 权限派生：是否拥有写权限（super 或 operator）。
    canWrite: () => { const role = get().user?.role; return role === 'super' || role === 'operator'; },
  };
});
