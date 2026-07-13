DROP TABLE IF EXISTS levels;
DROP TABLE IF EXISTS user_task;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS sign_record;
DROP TABLE IF EXISTS point_record;
DROP TABLE IF EXISTS exchange_record;
DROP TABLE IF EXISTS shop_items;
DROP TABLE IF EXISTS user_items;
DROP TABLE IF EXISTS level_unlock;
DROP TABLE IF EXISTS login_token;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS config;
DROP TABLE IF EXISTS leaderboard;
DROP TABLE IF EXISTS backup_save_log;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS app_version;
DROP TABLE IF EXISTS matchmaking_queue;
DROP TABLE IF EXISTS game_commands;
DROP TABLE IF EXISTS admin_audit_log;
DROP TABLE IF EXISTS level_tiles;
DROP TABLE IF EXISTS backup_unlocked_levels;
DROP TABLE IF EXISTS backup_save_items;
DROP TABLE IF EXISTS admin_audit_changes;

CREATE TABLE users (
    id TEXT PRIMARY KEY,
    phone TEXT UNIQUE,
    username TEXT NOT NULL,
    points INTEGER DEFAULT 0,
    password_hash TEXT,
    avatar_url TEXT,
    created_at INTEGER NOT NULL,
    role TEXT DEFAULT 'user',
    is_banned INTEGER DEFAULT 0
);

CREATE TABLE login_token (
    phone TEXT PRIMARY KEY,
    code TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE level_unlock (
    user_id TEXT,
    level_id INTEGER,
    unlocked_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, level_id),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE user_items (
    user_id TEXT,
    item_type TEXT,
    count INTEGER DEFAULT 0,
    PRIMARY KEY (user_id, item_type),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE shop_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    image_url TEXT,
    "group" TEXT,
    item_type TEXT NOT NULL,
    points_price INTEGER NOT NULL,
    stock INTEGER DEFAULT 100
);

CREATE TABLE exchange_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    shop_item_id INTEGER NOT NULL,
    item_type TEXT NOT NULL,
    count INTEGER NOT NULL,
    points_cost INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(shop_item_id) REFERENCES shop_items(id)
);

CREATE TABLE point_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    type TEXT NOT NULL,
    amount INTEGER NOT NULL,
    source TEXT NOT NULL,
    remaining_points INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE sign_record (
    user_id TEXT,
    sign_date TEXT,
    streak INTEGER NOT NULL,
    points_rewarded INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, sign_date),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE task (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    target_count INTEGER NOT NULL,
    points_reward INTEGER NOT NULL
);

CREATE TABLE user_task (
    user_id TEXT,
    task_id TEXT,
    task_date TEXT,
    progress INTEGER DEFAULT 0,
    is_completed INTEGER DEFAULT 0,
    is_rewarded INTEGER DEFAULT 0,
    PRIMARY KEY (user_id, task_id, task_date),
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(task_id) REFERENCES task(id)
);

CREATE TABLE notice (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    type TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

-- 管理后台操作审计日志（仅由后台写接口 INSERT，无任何写/删接口）
CREATE TABLE IF NOT EXISTS admin_audit_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    admin_id        TEXT    NOT NULL,
    admin_phone     TEXT    NOT NULL,
    admin_role      TEXT    NOT NULL,
    action          TEXT    NOT NULL,
    target_type     TEXT,
    target_id       TEXT,
    source_ip       TEXT,
    user_agent      TEXT,
    created_at      INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_admin ON admin_audit_log(admin_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON admin_audit_log(created_at);

CREATE TABLE leaderboard (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    level_id INTEGER NOT NULL,
    score INTEGER NOT NULL,
    clear_time_ms INTEGER NOT NULL,
    game_mode INTEGER NOT NULL DEFAULT 0,
    achieved_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

-- 按模式 + 关卡 + 分数建索引，加速分榜查询（无尽模式 game_mode=1 查询）
CREATE INDEX idx_leaderboard_mode ON leaderboard(game_mode, level_id, score DESC);

-- 按 user_id 建索引，加速以用户为维度的查询/统计（积分流水、兑换记录、个人榜等）
CREATE INDEX IF NOT EXISTS idx_leaderboard_user ON leaderboard(user_id);
-- 按 achieved_at 建索引，加速每日弹窗按时间窗口(>= / <=)过滤的聚合查询（昨日 TOP3、per-user 昨日排名）。
-- 注意：线上 leaderboard 为不断增长的大表，建索引会触发全表扫描/重写，需由 DBA 在业务低峰期执行；
-- IF NOT EXISTS 保证该语句幂等，可安全重复执行。
CREATE INDEX IF NOT EXISTS idx_leaderboard_achieved ON leaderboard(achieved_at);
CREATE INDEX IF NOT EXISTS idx_point_record_user ON point_record(user_id);
CREATE INDEX IF NOT EXISTS idx_exchange_record_user ON exchange_record(user_id);

-- ===== 迁移脚本（适用于已存在的库；DBA 执行，幂等）=====
-- ALTER TABLE leaderboard ADD COLUMN game_mode INTEGER NOT NULL DEFAULT 0;
-- 说明：0 = 闯关/PvP, 1 = 无尽生存；DEFAULT 0 兼容历史数据。
-- 若 CREATE TABLE 已包含 game_mode 列，则跳过 ALTER，仅补索引：
-- CREATE INDEX IF NOT EXISTS idx_leaderboard_mode ON leaderboard(game_mode, level_id, score DESC);

CREATE TABLE levels (
    level_id INTEGER PRIMARY KEY,
    difficulty INTEGER,
    created_at INTEGER NOT NULL
);

CREATE TABLE backup_save_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    points INTEGER,    -- 备份时的积分标量（save_data 拆表后由 save_data.points 提升）
    created_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_backup_created ON backup_save_log(created_at);

-- ===================== JSON 字符串列拆表：子表（T01，幂等建表） =====================

-- 关卡 tile 子表（levels.layout_data 拆表）
CREATE TABLE IF NOT EXISTS level_tiles (
    level_id            INTEGER NOT NULL,
    tile_index          INTEGER NOT NULL,
    tile_id             TEXT    NOT NULL,
    x                   INTEGER,
    y                   INTEGER,
    z                   INTEGER,
    "type"              INTEGER,
    is_blind            INTEGER DEFAULT 0,
    sealed_count        INTEGER DEFAULT 0,
    seal_unlock_threshold INTEGER,
    PRIMARY KEY (level_id, tile_index),
    FOREIGN KEY(level_id) REFERENCES levels(level_id)
);
CREATE INDEX IF NOT EXISTS idx_level_tiles_level ON level_tiles(level_id);

-- 备份解锁关卡子表（backup_save_log.save_data.unlocked_levels 拆表）
CREATE TABLE IF NOT EXISTS backup_unlocked_levels (
    backup_id INTEGER NOT NULL,
    level_id INTEGER NOT NULL,
    PRIMARY KEY (backup_id, level_id),
    FOREIGN KEY(backup_id) REFERENCES backup_save_log(id)
);
CREATE INDEX IF NOT EXISTS idx_backup_unlock_backup ON backup_unlocked_levels(backup_id);

-- 备份道具子表（backup_save_log.save_data.items 拆表）
CREATE TABLE IF NOT EXISTS backup_save_items (
    backup_id  INTEGER NOT NULL,
    item_type  TEXT    NOT NULL,
    count      INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (backup_id, item_type),
    FOREIGN KEY(backup_id) REFERENCES backup_save_log(id)
);
CREATE INDEX IF NOT EXISTS idx_backup_items_backup ON backup_save_items(backup_id);

-- 审计变更子表（admin_audit_log.before/after_snapshot 拆表）
CREATE TABLE IF NOT EXISTS admin_audit_changes (
    change_id INTEGER NOT NULL,
    field     TEXT    NOT NULL,
    old_val   TEXT,
    new_val   TEXT,
    PRIMARY KEY (change_id, field),
    FOREIGN KEY(change_id) REFERENCES admin_audit_log(id)
);
CREATE INDEX IF NOT EXISTS idx_audit_changes_change ON admin_audit_changes(change_id);

-- 导入初始多语言配置数据
INSERT INTO task (id, name, description, target_count, points_reward) VALUES 
('PLAY_3_GAMES', '小试牛刀', '游玩3局游戏', 3, 20),
('PLAY_5_GAMES', '大显身手', '游玩5局游戏', 5, 40),
('SIGN_IN_ONCE', '每日晨曦', '完成一次签到', 1, 10);

INSERT INTO notice (title, content, type, created_at) VALUES 
('消了个爽1.0盛大开启', '喜迎全新水墨修真版本，多重福利送不停！', 'ACTIVITY', 1720000000000),

INSERT INTO shop_items (name, description, item_type, points_price, stock) VALUES 
('乾坤符 (Undo)', '撤销上一步操作', 'UNDO', 20, 100),
('缩地咒 (MoveOut)', '从卡槽移出前三张卡牌', 'MOVEOUT', 30, 80),
('流沙契 (Shuffle)', '重新打乱桌面剩余卡牌', 'SHUFFLE', 20, 99),
('还魂丹 (Revive)', '消除失败时免除惩罚复活', 'REVIVE', 50, 20),
('天眼符 (Hint)', '自动高亮出一组可消卡牌', 'HINT', 15, 150),
('雷震子 (Bomb)', '直接炸毁卡槽中最后2张卡牌', 'BOMB', 40, 30),
('太极牌 (Joker)', '作为任意图案卡牌与前两张直接凑对消除', 'JOKER', 60, 10),
('双倍符 (Double)', '结算时获得双倍积分卡', 'DOUBLE_POINTS', 25, 50);

INSERT INTO config (key, value) VALUES 
('level_2_unlock_points', '50'),
('level_3_unlock_points', '100'),
('level_4_unlock_points', '200'),
('sign_rewards', '20,20,30,30,40,50,100'),
('gamemode_stage', 'on'),               -- 方案 B：闯关模式（默认开）
('gamemode_battle', 'on'),               -- 方案 B：对战模式（默认开）
('gamemode_endless', 'off');            -- 方案 B：无尽生存模式（默认关）

CREATE TABLE IF NOT EXISTS app_version (
    version_code INTEGER PRIMARY KEY,
    version_name TEXT NOT NULL,
    apk_url TEXT NOT NULL,
    download_url TEXT,                    -- 方案 B：独立下载链接（外部URL 或 R2 直链）
    update_log TEXT,
    is_force_update INTEGER DEFAULT 0,
    status INTEGER DEFAULT 0,             -- 方案 B：0=草稿 1=已发布 2=已下线（仅已发布对 check-update 生效）
    release_time INTEGER,                 -- 方案 B：发布时间(ms)，发布时写入
    created_at INTEGER NOT NULL
);

INSERT INTO app_version (version_code, version_name, apk_url, update_log, is_force_update, created_at) VALUES
(1, '1.0.0', 'https://pub-xxxxxx.r2.dev/sheeps_v1.0.0.apk', '初始版本发布', 0, 1720000000000);

INSERT INTO shop_items (id, name, description, item_type, points_price, stock) VALUES 
(1000, '河南·省味 (卡牌皮肤)', '解锁河南省特色美食图标皮肤', 'SKIN_HENAN', 200, 9999),
(1001, '四川·省味 (卡牌皮肤)', '解锁四川省特色美食图标皮肤', 'SKIN_SICHUAN', 200, 9999),
(2000, '萌趣竞技 (卡牌皮肤)', '12种萌系小羊卡牌，爽爽蓝边框搭配阳光金装饰', 'SKIN_SHUANG', 300, 9999),
(2001, '数码潮玩 (卡牌皮肤)', '12款数码设备卡面，科技蓝边框搭配霓虹装饰', 'SKIN_ELECTRONIC', 300, 9999),
(2002, '日常好物 (卡牌皮肤)', '12款日常用品卡面，暖橙边框搭配阳光装饰', 'SKIN_DAILY', 300, 9999),
(2003, '蔬菜园 (卡牌皮肤)', '12款新鲜蔬菜卡面，清新绿边框搭配田园装饰', 'SKIN_VEGETABLE', 300, 9999),
(2004, '水果盘 (卡牌皮肤)', '12款时令水果卡面，鲜红边框搭配果香装饰', 'SKIN_FRUIT', 300, 9999);


-- 联机对战匹配队列
CREATE TABLE IF NOT EXISTS matchmaking_queue (
    player_id TEXT PRIMARY KEY,
    joined_at INTEGER NOT NULL,
    matched_game_id TEXT,
    matched_opponent TEXT,
    game_seed INTEGER,
    duel_level INTEGER
);

-- 跨实例 WebSocket 消息转发队列
CREATE TABLE IF NOT EXISTS game_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    game_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    command_data TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_game_commands_game_id ON game_commands(game_id);

-- ===================== 多语言归一化表（方案 B，幂等） =====================
CREATE TABLE IF NOT EXISTS i18n_strings (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    str_key    TEXT    NOT NULL,                 -- 归一化键：{module}.{entity_ref}.{field}
    locale     TEXT    NOT NULL DEFAULT 'zh',    -- zh/en/tw/ja/ko
    module     TEXT    NOT NULL DEFAULT 'system',-- shop_items/task/notice/app_version/system/gamemode
    category   TEXT,                             -- 子分组（可选）：如 shop_items 的 "group" 系列
    value      TEXT,                             -- 文案内容
    updated_at INTEGER,
    UNIQUE(str_key, locale, module, category)
);
CREATE INDEX IF NOT EXISTS idx_i18n ON i18n_strings(module, category, locale);
CREATE INDEX IF NOT EXISTS idx_i18n_key ON i18n_strings(str_key, locale);

-- ===================== 图片资源 CDN（v2 增补，幂等） =====================
-- 1) 每皮肤 12 张卡面 URL（按 tile_index 1..12 排序）
CREATE TABLE IF NOT EXISTS skin_tiles (
    skin_type   TEXT    NOT NULL,   -- 渲染键，= item_type 去掉 "SKIN_" 前缀并小写，如 'henan'
    tile_index  INTEGER NOT NULL,   -- 1..12
    image_url   TEXT    NOT NULL,
    PRIMARY KEY (skin_type, tile_index)
);
CREATE INDEX IF NOT EXISTS idx_skin_tiles_type ON skin_tiles(skin_type);

-- 2) 道具图标单一数据源（真身）。item_type 与 shop_items.item_type 对齐（如 'UNDO'）
CREATE TABLE IF NOT EXISTS item_icons (
    item_type  TEXT    NOT NULL PRIMARY KEY,
    image_url  TEXT    NOT NULL
);

-- 3) shop_items 增加分组列（主题系列；可空）。GROUP 为 SQL 保留字，建表已用双引号包裹。
--    已上线库若未含该列，需用 `wrangler d1 execute` 手动 ALTER 补齐（迁移逻辑已从 Worker 移除）。
-- ===================== 生产迁移注释（DBA 执行，幂等） =====================
-- ALTER TABLE shop_items ADD COLUMN "group" TEXT;
-- 说明：分组维度默认为"主题系列"（地域系列/萌系系列/数码系列/生活系列），可空。
-- 已上线库若无 item_icons / skin_tiles 表，按上方 CREATE TABLE IF NOT EXISTS 补齐即可
-- （迁移逻辑已从 Worker 移除，新表需手动执行上方 CREATE 语句）。

