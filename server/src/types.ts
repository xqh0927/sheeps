/**
 * Cloudflare Worker 的环境变量与资源绑定接口
 * 在 Wrangler.toml 中配置的绑定项都需要在这里声明类型
 */
export interface Env {
  DB: D1Database;          // D1 关系型数据库实例
  SHEEPS_CACHE: KVNamespace; // KV 键值对缓存实例
  AVATAR_BUCKET: R2Bucket;   // R2 对象存储 bucket（用户头像）
  R2_PUBLIC_URL: string;     // R2 公网自定义域名（如 https://file.xqh.cc.cd）
  JWT_SECRET: string;        // JWT 签名密钥（Worker Secrets 注入）
  AES_KEY_HEX: string;       // AES-256 密钥（HEX，Worker Secrets 注入）
  ADMIN_WEB_ORIGIN: string;  // 管理后台 Pages 站点源（用于 CORS 精确授权）
}

/** GitHub Release 资源附件 */
export type GitHubReleaseAsset = {
  name?: string;
  browser_download_url?: string;
};

/** GitHub Release 接口响应体 */
export type GitHubRelease = {
  tag_name?: string;
  name?: string;
  body?: string;
  draft?: boolean;
  prerelease?: boolean;
  assets?: GitHubReleaseAsset[];
};

/** APP 客户端更新下发负载 */
export type AppUpdatePayload = {
  has_update: boolean;     // 是否有新版本
  version_name?: string;   // 版本名称 (如 v1.2.0)
  apk_url?: string;        // APK 下载直链
  update_log?: string;     // 更新日志摘要
  force_update?: boolean;  // 是否强制用户更新
};

/** APK 存在性检查的缓存状态 */
export interface ApkStatus {
  exists: boolean;         // APK 链接是否有效 (HTTP 200)
  checkedAt: number;       // 最后检查时间戳
}

/** 关卡生成：3D 坐标点 */
export interface Point3D {
  x: number;
  y: number;
  z: number; // 层级高度
}

/** 关卡生成：单个方块/卡牌的数据结构 */
export interface TileData {
  id: string;        // 唯一标识符
  x: number;
  y: number;
  z: number;
  type: number;      // 卡牌图案类型 (对应 UI 上的图标)
  isBlind: boolean;  // 是否为盲盒卡牌
  sealedCount: number; // 封印层数 (需要额外消除的次数)
  sealUnlockThreshold?: number; // 封印门控解锁阈值（每消除 N 张正常牌解锁 1 张封印），固定为 3，双端同步
}

/** 关卡 tile 子表行（levels.layout_data 拆表后的规范化存储） */
export interface LevelTileRow {
  level_id: number;            // 外键 → levels.level_id
  tile_index: number;          // 数组下标，从 0 起，用于稳定排序/重组
  tile_id: string;             // 原 TileData.id
  x: number;
  y: number;
  z: number;
  type: number;                // 原 TileData.type（SQL 关键字，建表用双引号包裹）
  is_blind: number;            // 原 TileData.isBlind（0/1）
  sealed_count: number;        // 原 TileData.sealedCount
  seal_unlock_threshold: number | null; // 原 TileData.sealUnlockThreshold（可空）
}

/** 备份解锁关卡子表行（backup_save_log.save_data.unlocked_levels 拆表） */
export interface BackupUnlockedLevelRow {
  backup_id: number;           // 外键 → backup_save_log.id
  level_id: number;            // 原 unlocked_levels[] 元素
}

/** 备份道具子表行（backup_save_log.save_data.items 拆表） */
export interface BackupItemRow {
  backup_id: number;           // 外键 → backup_save_log.id
  item_type: string;           // 原 items[].item_type
  count: number;               // 原 items[].count
}

/** 审计变更子表行（admin_audit_log.before/after_snapshot 拆表） */
export interface AuditChangeRow {
  change_id: number;           // 外键 → admin_audit_log.id
  field: string;               // 被变更字段名
  old_val: string | null;      // 变更前值，JSON.stringify 后的文本（NULL 表示字段在 before 中不存在）
  new_val: string | null;      // 变更后值，JSON.stringify 后的文本（NULL 表示字段在 after 中不存在）
}

/** WebSocket 玩家会话状态 */
export interface PlayerSession {
  playerId: string;
  socket: WebSocket;
  lastActionTime: number; // 上次心跳或操作时间，用于限流防刷
  status: 'ACTIVE' | 'DISCONNECTED';
  disconnectTimer?: any;  // 断线重连倒计时器
}

/**
 * 难度信息——getDifficultyForLevel 的返回结构
 */
export interface DifficultyInfo {
  /** 难度系数 1~100 */
  difficulty: number;

  /** 当前关卡在本难度段内的序号（0-based） */
  subIndex: number;

  /** 本难度段共覆盖多少关（1 或 2，由用户种子决定） */
  totalSubLevels: number;
}

/**
 * 卡牌数量计算结果
 */
export interface CardCountResult {
  /** 最终卡牌数量（3 的倍数） */
  cardCount: number;

  /** 是否为休息关卡 */
  isRestLevel: boolean;

  /** 难度星级 1~5 */
  stars: number;

  /** 难度标签文案 */
  label: string;
}
