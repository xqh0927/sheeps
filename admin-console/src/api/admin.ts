import { api } from './client';

export interface PageResult<T> {
  success: boolean;
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface AdminUserRow {
  id: string;
  phone: string;
  username: string;
  points: number;
  role: string;
  is_banned: number;
  avatar_url?: string;
  created_at: number;
}

export interface AccountRow {
  id: string;
  phone: string;
  username: string;
  role: string;
  is_banned: number;
  created_at: number;
}

export interface AuditRow {
  id: number;
  admin_id: string;
  admin_phone: string;
  admin_role: string;
  action: string;
  target_type: string | null;
  target_id: string | null;
  before_snapshot: string | null;
  after_snapshot: string | null;
  source_ip: string;
  user_agent: string;
  created_at: number;
}

export interface Stats {
  users_total: number;
  today_signup: number;
  exchange_total: number;
  points_total: number;
  notice_count: number;
  banned_count: number;
  shop_item_count: number;
  task_count: number;
  level_count: number;
  endless_play_count: number;
  endless_max_score: number;
}

const buildQuery = (params: Record<string, string | number | undefined>): string => {
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== '') usp.set(k, String(v));
  });
  const s = usp.toString();
  return s ? `?${s}` : '';
};

// ============ 统计 ============
export const getStats = () => api.get<Stats>('/api/admin/stats').then((r) => r.data);

// ============ 用户管理 ============
export const listUsers = (page = 1, pageSize = 20, keyword = '') =>
  api.get<PageResult<AdminUserRow>>(`/api/admin/users${buildQuery({ page, pageSize, keyword })}`).then((r) => r.data);

export const adjustPoints = (id: string, amount: number, reason?: string) =>
  api.post(`/api/admin/users/${id}/points`, { amount, reason }).then((r) => r.data);

export const banUser = (id: string, banned: boolean) =>
  api.post(`/api/admin/users/${id}/ban`, { banned }).then((r) => r.data);

export const renameUser = (id: string, username: string) =>
  api.put(`/api/admin/users/${id}`, { username }).then((r) => r.data);

export interface UserItem {
  item_type: string;
  count: number;
}

export const listUserItems = (id: string) =>
  api.get<{ success: boolean; list: UserItem[] }>(`/api/admin/users/${id}/items`).then((r) => r.data);

export const updateUserItems = (id: string, items: UserItem[]) =>
  api.post(`/api/admin/users/${id}/items`, { items }).then((r) => r.data);

// ============ 商品 ============
export const listShopItems = (page = 1, pageSize = 20) =>
  api.get<PageResult<any>>(`/api/admin/shop-items${buildQuery({ page, pageSize })}`).then((r) => r.data);

export const createShopItem = (body: Record<string, any>) =>
  api.post('/api/admin/shop-items', body).then((r) => r.data);

export const updateShopItem = (id: string | number, body: Record<string, any>) =>
  api.put(`/api/admin/shop-items/${id}`, body).then((r) => r.data);

export const deleteShopItem = (id: string | number) =>
  api.delete(`/api/admin/shop-items/${id}`).then((r) => r.data);

// ============ 公告 ============
export const listNotices = (page = 1, pageSize = 20) =>
  api.get<PageResult<any>>(`/api/admin/notices${buildQuery({ page, pageSize })}`).then((r) => r.data);

export const createNotice = (body: Record<string, any>) =>
  api.post('/api/admin/notices', body).then((r) => r.data);

export const updateNotice = (id: string | number, body: Record<string, any>) =>
  api.put(`/api/admin/notices/${id}`, body).then((r) => r.data);

export const deleteNotice = (id: string | number) =>
  api.delete(`/api/admin/notices/${id}`).then((r) => r.data);

// ============ 任务 ============
export const listTasks = (page = 1, pageSize = 20) =>
  api.get<PageResult<any>>(`/api/admin/tasks${buildQuery({ page, pageSize })}`).then((r) => r.data);

export const createTask = (body: Record<string, any>) =>
  api.post('/api/admin/tasks', body).then((r) => r.data);

export const updateTask = (id: string | number, body: Record<string, any>) =>
  api.put(`/api/admin/tasks/${id}`, body).then((r) => r.data);

export const deleteTask = (id: string | number) =>
  api.delete(`/api/admin/tasks/${id}`).then((r) => r.data);

// ============ 关卡 ============
export const listLevels = (page = 1, pageSize = 20) =>
  api.get<PageResult<any>>(`/api/admin/levels${buildQuery({ page, pageSize })}`).then((r) => r.data);

export const createLevel = (body: Record<string, any>) =>
  api.post('/api/admin/levels', body).then((r) => r.data);

export const updateLevel = (id: string | number, body: Record<string, any>) =>
  api.put(`/api/admin/levels/${id}`, body).then((r) => r.data);

export const deleteLevel = (id: string | number) =>
  api.delete(`/api/admin/levels/${id}`).then((r) => r.data);

// ============ 配置 ============
export const getConfig = () => api.get<{ success: boolean; list: { key: string; value: string }[] }>('/api/admin/config').then((r) => r.data);

export const updateConfig = (key: string, value: string) =>
  api.post('/api/admin/config', { key, value }).then((r) => r.data);

// ============ 管理员账户（super） ============
export const listAccounts = () => api.get<{ success: boolean; list: AccountRow[] }>('/api/admin/accounts').then((r) => r.data);

export const createAccount = (phone: string, role: string, password?: string) =>
  api.post('/api/admin/accounts', { phone, role, password }).then((r) => r.data);

export const setAccountRole = (id: string, role: string) =>
  api.put(`/api/admin/accounts/${id}/role`, { role }).then((r) => r.data);

export const disableAccount = (id: string, disabled: boolean) =>
  api.post(`/api/admin/accounts/${id}/disable`, { disabled }).then((r) => r.data);

// ============ 审计日志（super） ============
export const listAuditLogs = (params: {
  page?: number;
  pageSize?: number;
  admin_id?: string;
  action?: string;
  from?: number;
  to?: number;
}) => api.get<PageResult<AuditRow>>(`/api/admin/audit-logs${buildQuery(params as any)}`).then((r) => r.data);
