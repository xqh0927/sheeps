import { create } from 'zustand';
import { api, setTokens, setTokensRefreshedHandler } from '../api/client';

export type AdminRole = 'super' | 'operator' | 'readonly';

export interface AdminUser {
  id: string;
  phone: string;
  username: string;
  role: AdminRole;
}

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

const STORAGE_KEY = 'miadmin_auth';

interface PersistedAuth {
  token: string;
  refreshToken: string;
  user: AdminUser;
}

function persist(token: string | null, refresh: string | null, user: AdminUser | null): void {
  if (token && refresh && user) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, refreshToken: refresh, user }));
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}

export const useAuth = create<AuthState>((set, get) => {
  // 静默刷新成功后，同步内存态 + 持久化
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

    login: async (phone, password) => {
      const resp = await api.post('/api/admin/login', { phone, password });
      const { token, refreshToken, user } = resp.data;
      setTokens(token, refreshToken);
      set({ token, refreshTokenVal: refreshToken, user });
      persist(token, refreshToken, user);
      return user;
    },

    logout: () => {
      setTokens(null, null);
      set({ token: null, refreshTokenVal: null, user: null });
      persist(null, null, null);
    },

    isSuper: () => get().user?.role === 'super',
    canWrite: () => get().user?.role !== 'readonly',
  };
});
