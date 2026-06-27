# 国风消除游戏新增功能与架构重构设计规格说明书

本设计文档旨在指导将游戏从单机/简易版扩展至包含完整登录体系、积分与背包系统、签到机制、商城与任务模块的完整国风消消乐游戏。同时，对现有的 XML 布局进行彻底重构，实现 100% 纯 Jetpack Compose 的客户端架构。

---

## 1. 数据库模型设计 (D1 Database SQL Schema)

数据库将部署在 Cloudflare D1，新增/重构以下 12 张表：

```sql
-- 1. 用户基本信息表
CREATE TABLE users (
    id TEXT PRIMARY KEY,            -- 用户唯一ID (JWT Subject/UUID)
    phone TEXT UNIQUE,              -- 手机号（游客此项为NULL）
    username TEXT NOT NULL,         -- 昵称
    avatar TEXT,                    -- 头像URL
    points INTEGER DEFAULT 0,       -- 当前积分
    created_at INTEGER NOT NULL
);

-- 2. 验证码与登录令牌辅助表
CREATE TABLE login_token (
    phone TEXT PRIMARY KEY,         -- 手机号
    code TEXT NOT NULL,             -- 临时验证码
    created_at INTEGER NOT NULL     -- 发送时间戳（用于验证过期，如5分钟）
);

-- 3. 关卡解锁关系表
CREATE TABLE level_unlock (
    user_id TEXT,
    level_id INTEGER,
    unlocked_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, level_id),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

-- 4. 玩家背包道具表
CREATE TABLE user_items (
    user_id TEXT,
    item_type TEXT,                 -- 道具类型: UNDO, SHUFFLE, MOVEOUT, REVIVE, HINT, BOMB, JOKER, DOUBLE_POINTS
    count INTEGER DEFAULT 0,        -- 数量
    PRIMARY KEY (user_id, item_type),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

-- 5. 商城商品配置表
CREATE TABLE shop_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    image_url TEXT,
    item_type TEXT NOT NULL,
    points_price INTEGER NOT NULL,  -- 所需积分
    stock INTEGER DEFAULT 100       -- 库存数
);

-- 6. 道具兑换历史记录表
CREATE TABLE exchange_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    shop_item_id INTEGER NOT NULL,
    item_type TEXT NOT NULL,
    count INTEGER NOT NULL,
    points_cost INTEGER NOT NULL,   -- 消耗积分数
    created_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(shop_item_id) REFERENCES shop_items(id)
);

-- 7. 积分变动流水表
CREATE TABLE point_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    type TEXT NOT NULL,             -- 变动方向: IN (获取), OUT (消耗)
    amount INTEGER NOT NULL,        -- 变动积分值
    source TEXT NOT NULL,           -- 来源类型: FIRST_CLEAR (首次通关), SIGN_IN (签到), DAILY_TASK (每日任务), SYSTEM_GIFT (系统赠送), UNLOCK_LEVEL (提前解锁关卡), SHOP_REDEEM (商城兑换)
    remaining_points INTEGER NOT NULL, -- 剩余积分
    created_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

-- 8. 签到历史记录表
CREATE TABLE sign_record (
    user_id TEXT,
    sign_date TEXT,                 -- 签到日期 (格式 YYYY-MM-DD)
    streak INTEGER NOT NULL,        -- 连续签到天数
    points_rewarded INTEGER NOT NULL, -- 本次奖励积分
    created_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, sign_date),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

-- 9. 每日任务定义表
CREATE TABLE task (
    id TEXT PRIMARY KEY,            -- 任务ID: PLAY_3_GAMES, PLAY_5_GAMES, SIGN_IN_ONCE
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    target_count INTEGER NOT NULL,  -- 目标次数
    points_reward INTEGER NOT NULL  -- 奖励积分值
);

-- 10. 用户任务进度完成表
CREATE TABLE user_task (
    user_id TEXT,
    task_id TEXT,
    task_date TEXT,                 -- 任务日期 (格式 YYYY-MM-DD)
    progress INTEGER DEFAULT 0,      -- 当前进度
    is_completed INTEGER DEFAULT 0,  -- 是否已完成 (0/1)
    is_rewarded INTEGER DEFAULT 0,   -- 是否已领取奖励 (0/1)
    PRIMARY KEY (user_id, task_id, task_date),
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(task_id) REFERENCES task(id)
);

-- 11. 公告配置表
CREATE TABLE notice (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    type TEXT NOT NULL,             -- 公告类型: ACTIVITY, UPDATE, MAINTENANCE
    created_at INTEGER NOT NULL
);

-- 12. 系统全局参数配置表
CREATE TABLE config (
    key TEXT PRIMARY KEY,           -- 配置名: level_2_unlock_points...
    value TEXT NOT NULL             -- 配置值
);
```

---

## 2. API 路由设计与身份校验 (HTTP REST API & JWT)

所有受保护接口通过 `Authorization: Bearer <JWT>` 进行身份确认。

### 2.1 认证与同步接口
*   `POST /api/auth/send-code`：手机号获取 6 位数字验证码（演示环境将在 JSON 直接返回）。
*   `POST /api/auth/login`：手机号 + 验证码登录。生成并返回 JWT，同时返回用户信息、积分、已解锁关卡列表、背包道具数量、签到状态。
*   `POST /api/user/sync` (Auth)：登录成功时，客户端可选将本地的游客离线进度（如前3关已解锁状态）上报云端同步。
*   `POST /api/user/rename` (Auth)：修改用户昵称。

### 2.2 关卡解锁与结算接口
*   `POST /api/level/unlock` (Auth)：消耗积分子模块，提前解锁后续关卡。
*   `POST /api/score/submit`：提交关卡分数。登录状态下写入排行榜、自动解锁下一关、并上报每日任务游玩进度。

### 2.3 商城与道具接口
*   `GET /api/shop/items`：获取商城全部在售商品（图片、价格、库存等）。
*   `POST /api/shop/exchange` (Auth)：兑换道具。成功后自动扣除积分，增加背包。

### 2.4 签到与任务接口
*   `POST /api/sign/today` (Auth)：每日签到。发放配置积分（连续签到 1-7 天递增），并记录流水。
*   `GET /api/task/daily` (Auth)：获取用户今日的任务清单及进度。
*   `POST /api/task/claim` (Auth)：领取已完成任务的积分奖励。

### 2.5 公告与系统配置
*   `GET /api/notice/list`：获取公告列表。
*   `GET /api/admin/config` 与 `POST /api/admin/config`：读取与修改后台系统配置（例如：签到奖励点数、章节提前解锁积分配置）。

---

## 3. 客户端架构设计 (MVI & 100% Compose)

为了保证客户端的高内聚、低耦合，我们进行以下改动：

### 3.1 废弃 XML 与 ViewBinding
1.  **废弃 `BaseBindingActivity`**：`SplashActivity` 和 `LeaderboardActivity` 迁移至继承 `BaseActivity`，采用 `setContent {}` 渲染 Compose 组件。
2.  **删除布局 XML**：删除 `activity_splash.xml`、`activity_leaderboard.xml` 和 `item_ranking.xml`。
3.  **删除 `LeaderboardAdapter`**：用 Compose `LazyColumn` 代替 RecyclerView 适配器。

### 3.2 导航体系 (Multi-Tab Homepage)
主页面 `MenuActivity` 作为应用主容器，底部使用 Compose `NavigationBar` 管理三个 Tab 页：
*   **游戏 Tab (`GameHomeTab`)**：展示公告、关卡列表。点击解锁关卡弹出“备战弹窗”进行道具携带。
*   **商城 Tab (`ShopTab`)**：网格展示道具购买，积分兑换。
*   **个人中心 Tab (`PersonalTab`)**：展示 UID、头像、积分流水、兑换记录、退出登录、设置等。

### 3.3 游客限制策略
若 MMKV 中未检测到有效 Token：
*   用户处于游客模式。只能解锁并游玩 1、2、3 关。
*   点击第 4 关及后续关卡、点击商城兑换、点击签到或查看任务时，均会被拦截并弹出**手机号+验证码登录弹窗**。

---

## 4. 日志审计与网络拦截器 (OkHttp HttpLoggingInterceptor)

在客户端 `NetworkModule` 中，我们将注入自定义拦截器以提供完整的请求日志：
*   **请求日志**：打印 Request URL、HTTP Method、Headers 列表、Request Body 文本。
*   **响应日志**：打印 Response Code、Response Message、耗时（毫秒）、Response Body 文本。
*   **错误日志**：对于捕获的 Socket 异常或 SSL 握手异常，打印出完整的 Exception Message。

---

## 5. 验证方案

### 5.1 自动化验证
*   编写 `SOLVABILITY` 验证测试，确保逆向关卡的可解性。
*   编写 JWT 签发与解密单元测试，确保后端拦截非法或过期的 Token。

### 5.2 手动验证
*   **游客限制**：未登录时，点击第 4 关确认弹出登录框，且无法进行签到与兑换。
*   **登录同步**：登录成功后，能够拉取云端积分和背包数量，且通关后成绩能够即时保存，且退出登录后恢复游客模式。
