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
export const listUsers = (page = 1, pageSize = 20, keyword = '', role = '') =>
  api.get<PageResult<AdminUserRow>>(`/api/admin/users${buildQuery({ page, pageSize, keyword, role })}`).then((r) => r.data);

export const adjustPoints = (id: string, amount: number, reason?: string) =>
  api.post(`/api/admin/users/${id}/points`, { amount, reason }).then((r) => r.data);

export const banUser = (id: string, banned: boolean) =>
  api.post(`/api/admin/users/${id}/ban`, { banned }).then((r) => r.data);

export const renameUser = (id: string, username: string) =>
  api.put(`/api/admin/users/${id}`, { username }).then((r) => r.data);

export const deleteUser = (id: string) =>
  api.delete(`/api/admin/users/${id}`).then((r) => r.data);

export interface UserItem {
  item_type: string;
  count: number;
}

export const listUserItems = (id: string) =>
  api.get<{ success: boolean; list: UserItem[] }>(`/api/admin/users/${id}/items`).then((r) => r.data);

export const updateUserItems = (id: string, items: UserItem[]) =>
  api.post(`/api/admin/users/${id}/items`, { items }).then((r) => r.data);

// ============ 商品 ============
export const listShopItems = (page = 1, pageSize = 20, keyword = '') =>
  api.get<PageResult<any>>(`/api/admin/shop-items${buildQuery({ page, pageSize, keyword })}`).then((r) => r.data);

export const createShopItem = (body: Record<string, any>) =>
  api.post('/api/admin/shop-items', body).then((r) => r.data);

export const updateShopItem = (id: string | number, body: Record<string, any>) =>
  api.put(`/api/admin/shop-items/${id}`, body).then((r) => r.data);

export const deleteShopItem = (id: string | number) =>
  api.delete(`/api/admin/shop-items/${id}`).then((r) => r.data);

// ============ 公告 ============
export const listNotices = (page = 1, pageSize = 20, keyword = '') =>
  api.get<PageResult<any>>(`/api/admin/notices${buildQuery({ page, pageSize, keyword })}`).then((r) => r.data);

export const createNotice = (body: Record<string, any>) =>
  api.post('/api/admin/notices', body).then((r) => r.data);

export const updateNotice = (id: string | number, body: Record<string, any>) =>
  api.put(`/api/admin/notices/${id}`, body).then((r) => r.data);

export const deleteNotice = (id: string | number) =>
  api.delete(`/api/admin/notices/${id}`).then((r) => r.data);

// ============ 任务 ============
export const listTasks = (page = 1, pageSize = 20, keyword = '') =>
  api.get<PageResult<any>>(`/api/admin/tasks${buildQuery({ page, pageSize, keyword })}`).then((r) => r.data);

export const createTask = (body: Record<string, any>) =>
  api.post('/api/admin/tasks', body).then((r) => r.data);

export const updateTask = (id: string | number, body: Record<string, any>) =>
  api.put(`/api/admin/tasks/${id}`, body).then((r) => r.data);

export const deleteTask = (id: string | number) =>
  api.delete(`/api/admin/tasks/${id}`).then((r) => r.data);

// ============ 关卡 ============
export const listLevels = (page = 1, pageSize = 20, keyword = '') =>
  api.get<PageResult<any>>(`/api/admin/levels${buildQuery({ page, pageSize, keyword })}`).then((r) => r.data);

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

// ============ 图片资源（上传 / 皮肤卡面 / 道具图标）============

/** 商品分组系列常量（与 Android 共用，主题系列维度） */
export const SHOP_GROUPS = ['地域系列', '萌系系列', '数码系列', '生活系列'];

export interface SkinTile {
  tile_index: number;
  image_url: string | null;
}

/**
 * 通用图片上传：multipart/form-data 的 file + key（或 folder）-> R2 -> 返回 { url }。
 * 覆盖默认 JSON Content-Type，让浏览器自动补 multipart boundary。
 */
export const uploadImage = (formData: FormData): Promise<{ success: boolean; url: string }> =>
  api
    .post<{ success: boolean; url: string }>('/api/admin/upload-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data);

/** 获取某皮肤的 12 张卡面（预填用） */
export const getSkinTiles = (skinType: string) =>
  api.get<{ success: boolean; skin_type: string; tiles: SkinTile[] }>(`/api/admin/skin-tiles/${skinType}`).then((r) => r.data);

/** 批量保存某皮肤的 12 张卡面（第 1 张自动同步封面到 shop_items.image_url） */
export const saveSkinTiles = (skinType: string, tiles: SkinTile[]) =>
  api.put(`/api/admin/skin-tiles/${skinType}`, { tiles }).then((r) => r.data);

/** 保存某道具图标（写 item_icons 真身 + 镜像 shop_items.image_url） */
export const saveItemIcon = (itemType: string, imageUrl: string) =>
  api.put(`/api/admin/item-icons/${itemType}`, { image_url: imageUrl }).then((r) => r.data);

/** 获取某道具图标（预填用）；icon 为 null 表示尚未上传 */
export const getItemIcon = (itemType: string) =>
  api
    .get<{ success: boolean; item_type: string; icon: { item_type: string; image_url: string | null } | null }>(
      `/api/admin/item-icons/${itemType}`
    )
    .then((r) => r.data);

// ============ App 版本管理 ============
export interface AppVersionRow {
  version_code: number;
  version_name: string;
  apk_url: string;
  download_url: string | null;
  update_log: string | null;
  is_force_update: number;
  status: number;
  release_time: number | null;
  created_at: number;
}

export const listAppVersions = (page = 1, pageSize = 20) =>
  api.get<PageResult<AppVersionRow>>(`/api/admin/app-versions${buildQuery({ page, pageSize })}`).then((r) => r.data);

export const createAppVersion = (body: Record<string, any>) =>
  api.post('/api/admin/app-versions', body).then((r) => r.data);

export const updateAppVersion = (code: number | string, body: Record<string, any>) =>
  api.put(`/api/admin/app-versions/${code}`, body).then((r) => r.data);

export const deleteAppVersion = (code: number | string) =>
  api.delete(`/api/admin/app-versions/${code}`).then((r) => r.data);

/** 上传 APK 到 R2，返回 { url } 回填 download_url */
export const uploadApk = (formData: FormData): Promise<{ success: boolean; url: string }> =>
  api
    .post<{ success: boolean; url: string }>('/api/admin/upload-apk', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data);

// ============ 多语言统一管理（i18n_strings）============
export interface I18nRow {
  id: number;
  str_key: string;
  locale: string;
  module: string;
  category: string | null;
  value: string | null;
  updated_at: number | null;
  entity_label?: string | null;
}

export const listI18n = (params: { module?: string; locale?: string; keyword?: string; page?: number; pageSize?: number }) =>
  api.get<PageResult<I18nRow>>(`/api/admin/i18n${buildQuery(params as any)}`).then((r) => r.data);

export const createI18n = (body: Record<string, any>) =>
  api.post('/api/admin/i18n', body).then((r) => r.data);

export const updateI18n = (id: number | string, body: Record<string, any>) =>
  api.put(`/api/admin/i18n/${id}`, body).then((r) => r.data);

export const deleteI18n = (id: number | string) =>
  api.delete(`/api/admin/i18n/${id}`).then((r) => r.data);

// ============ 排行榜管理 ============
export interface LeaderboardRow {
  id: number;
  user_id: string;
  username: string;
  level_id: number;
  score: number;
  clear_time_ms: number;
  game_mode: number;
  achieved_at: number;
}

export const listLeaderboard = (params: { game_mode?: number; level_id?: number; page?: number; pageSize?: number }) =>
  api.get<PageResult<LeaderboardRow>>(`/api/admin/leaderboard${buildQuery(params as any)}`).then((r) => r.data);

export const createLeaderboardRow = (body: Record<string, any>) =>
  api.post('/api/admin/leaderboard', body).then((r) => r.data);

export const updateLeaderboardRow = (id: number | string, body: Record<string, any>) =>
  api.put(`/api/admin/leaderboard/${id}`, body).then((r) => r.data);

export const deleteLeaderboardRow = (id: number | string) =>
  api.delete(`/api/admin/leaderboard/${id}`).then((r) => r.data);

// ============ 用户搜索（反查 id，供排行榜 UserPicker）============
export interface UserSearchResult {
  id: string;
  username: string;
  phone: string;
}

export const searchUsers = (q: string) =>
  api.get<{ success: boolean; list: UserSearchResult[] }>(`/api/admin/users/search${buildQuery({ q })}`).then((r) => r.data);

// ============ 游戏模式开关 ============
/** 读取游戏模式开关状态（从 config 解析 gamemode_*） */
export const getGameModes = () =>
  getConfig().then((d) => {
    const map: Record<string, string> = {};
    (d.list || []).forEach((c: { key: string; value: string }) => {
      map[c.key] = c.value;
    });
    return { stage: map['gamemode_stage'] === 'on', endless: map['gamemode_endless'] === 'on', battle: map['gamemode_battle'] === 'on' };
  });

// ============ KV 缓存清理 ============
/** 清空 SHEEPS_CACHE KV 中所有缓存键 */
export const clearCache = () =>
  api.post<{ success: boolean; deleted: number }>('/api/admin/cache/clear').then((r) => r.data);

