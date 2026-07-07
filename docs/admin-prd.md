# 秘境消消乐 · 管理后台增量 PRD（简单 PRD 风格）

> 文档性质：产品需求文档（增量），不写实现代码，供架构师修订设计、工程师据以实现。
> 作者：产品经理许清楚 ｜ 版本：v0.1（增量）｜ 语言：中文
> 范围基线：完全对齐 `docs/system_design.md` v1.0 设计（架构师高见远）。本文档仅**补全 v1.0 尚未明确的业务规则**，不重复已有技术细节。

---

## 0. 项目信息与原始需求复述

| 项 | 内容 |
|---|---|
| Language | 中文 |
| Programming Language | 前端 React + Vite + MUI（与 v1.0 §1.2 一致）；后端复用现有 Cloudflare Worker（TypeScript） |
| Project Name | `admin_console`（新建前端站）；后端改动位于现有 `server/` |
| 原始需求 | 为国风消除游戏（Android + Worker 后端）**新增一个 Web 管理后台**，基于现有后端能力运营游戏数据。已确认决策：① Pages 独立站调 `/api/admin/*`；② 复用 `users` 表并做**权限分级**（非 is_admin 二值）；③ 需操作审计日志；④ 初始管理员用 seed 脚本一次性创建；⑤ 走标准 SOP，白嫖 Cloudflare 免费额度。 |

**与 v1.0 的关键协调点（重要）**：v1.0 在 `users` 表新增 `is_admin INTEGER`（二值）、JWT payload 用 `role:'admin'`，且 §8 第 6 项仍问"是否细分角色"。**本需求已确认要三级权限**。因此 v1.0 的 `is_admin` 模型需升级为 `role` 三级模型（详见 §4 与 §7），请架构师据此修订 T01 的 `migrateSchema`、JWT 签发与 `requireAdmin` 逻辑。

---

## 1. 产品目标

用**最低成本**给运营团队一个能**安全**管理游戏数据的 Web 后台，且整体落在 Cloudflare 免费额度内。

- **G1（安全）**：管理员分级鉴权 + 操作审计，杜绝越权与无记录改动。
- **G2（低成本）**：复用现有 Worker/D1/KV/R2 与 Cloudflare Pages 免费额度，不引入付费资源。
- **G3（可用）**：运营可在一个后台完成用户、商品、公告、任务、关卡、配置的日常管理与数据概览。

（正交性：G1 管权限与追溯，G2 管成本约束，G3 管业务覆盖，三者互不重叠。）

---

## 2. 用户故事（三类管理员视角）

- **超级管理员（super）**
  - 作为 super，我希望能管理所有内容数据**并**管理其他管理员账号（创建/分配角色/禁用），以便控制后台访问权限。
  - 作为 super，我希望能查看审计日志，以便追溯任何管理员的敏感操作。
  - 作为 super，我希望能读写系统配置，以便在紧急时调整运营参数。

- **运营（operator）**
  - 作为 operator，我希望能管理用户（改积分、封禁、改昵称）与商品/公告/任务/关卡/配置内容，以便日常运营，但**不能**触碰其他管理员账号。
  - 作为 operator，我希望所有写操作都被记录审计，以便出问题时可自证清白。
  - 作为 operator，我希望登录后直接进入仪表盘看到关键指标，以便快速掌握运营状态。

- **只读（readonly）**
  - 作为 readonly，我希望能查看所有运营页面（用户/商品/公告/任务/关卡/配置/仪表盘）的数据，以便监控与核对，但任何写按钮都禁用，避免误操作。
  - 作为 readonly，我希望即便尝试调用写接口也会被后端拒绝（403），以便权限不被绕过。
  - 作为 readonly，我不希望看到"管理员账户管理"和"审计日志"导航，以免误入无权限区域。

---

## 3. 需求池（P0 / P1 / P2）

> 优先级：P0=必须（本期上线）；P1=应当（强烈建议本期，视排期）；P2=可选/二期。

### P0（必须）

| 编号 | 需求 | 说明 |
|---|---|---|
| P0-1 | 管理员登录 + 鉴权 | `POST /api/admin/login`；手机号+密码校验 `role` 三级；签发 admin token（见 §4）；token 持久化 |
| P0-2 | 受保护路由 + 布局 | 未登录跳 `/login`；401 清 token 跳登录；侧边栏 + 顶栏；按角色显隐导航/按钮 |
| P0-3 | 角色权限模型（数据层） | `users` 表 `role TEXT`（super/operator/readonly），取代 `is_admin`；`requireAdmin` 三级校验 + 写操作 403 校验（见 §4、§7） |
| P0-4 | 仪表盘概览 | 用户总数、今日新增、兑换数、积分总量、公告数、封禁数等指标（接口见 v1.0 §3.2.8，字段扩展见 §6） |
| P0-5 | 用户管理 | 列表/搜索/分页；改积分（正负，需填原因）；封禁/解封；改昵称。接口见 v1.0 §3.2.2 |
| P0-6 | 商品管理 CRUD | shop_items 增删改查；接口见 v1.0 §3.2.3 |
| P0-7 | 公告管理 CRUD | notice 增删改查；接口见 v1.0 §3.2.4 |
| P0-8 | 任务管理 CRUD | task 增删改查；接口见 v1.0 §3.2.5 |
| P0-9 | 关卡管理 CRUD | levels 增删改查；`layout_data` 为 JSON 文本，前端提供 JSON 编辑器并做格式校验；接口见 v1.0 §3.2.6 |
| P0-10 | 系统配置读写 | 将 v1.0 §3.2.7 的 `/api/admin/config` 从 system 分支迁至 admin handler **并加鉴权**（原端点无鉴权，见 v1.0 §0 红色项） |
| P0-11 | 审计日志（数据层 + 落审计） | 新增 `admin_audit_log` 表；关键写操作 + 登录必须落审计（范围见 §5） |
| P0-12 | 初始化管理员 seed | `server/scripts/seed-admin.mjs` 一次性创建首个 `super` 账号（PBKDF2 密码哈希） |

### P1（应当）

| 编号 | 需求 | 说明 |
|---|---|---|
| P1-1 | 审计日志查看页 | super 可查看/按时间·操作人·动作类型筛选审计记录（只读，可导出 CSV） |
| P1-2 | 管理员账户管理页 | super 创建/分配角色/禁用其他管理员；支持将普通用户提升为 operator/readonly |
| P1-3 | 危险操作二次确认 | 改积分、封禁/解封、删除等用 MUI Dialog 二次确认（readonly 按钮已禁用） |
| P1-4 | 仪表盘图表 | 今日新增/兑换趋势等轻量图表（MUI Charts 或简单 SVG） |
| P1-5 | token 静默刷新 | access 过期用 refresh 静默续期，避免频繁重新登录 |
| P1-6 | 列表批量操作 | 如用户批量封禁（仅 super/operator） |

### P2（可选 / 二期）

| 编号 | 需求 | 说明 |
|---|---|---|
| P2-1 | 封禁实时拦截游戏端 | `is_banned` 在玩家鉴权（`getAuthenticatedUser`）处生效，封禁后立即无法游戏（本期仅数据标记） |
| P2-2 | 多语言字段后台维护 UI | 商品/公告/任务 `name_en/tw/ja/ko` 等由后台表单维护（本期仅默认语言） |
| P2-3 | 双因素认证（2FA） | 管理员登录增加 TOTP |
| P2-4 | 登录失败锁定 / 验证码 | 防暴力破解 |
| P2-5 | 配置项 schema 校验 UI | 配置按分组展示并做类型校验 |

---

## 4. 角色权限矩阵（重点）

### 4.1 角色模型定义（取代 v1.0 的 is_admin）

- `users` 表新增 **`role TEXT`**，取值 `super` / `operator` / `readonly`，默认 `readonly`。**取代 v1.0 的 `is_admin INTEGER`**（即"是否管理员" = `role != 'readonly'` 的派生，无需单独列）。保留 `is_banned INTEGER`。
- JWT payload `role` 携带**真实角色**（super/operator/readonly），不再是固定的 `'admin'`。
- 后端校验分两层：
  1. `requireAdmin(request, env)` → 校验 `type==='access' && role IN ('super','operator','readonly')`，否则 401。
  2. 写操作（POST/PUT/DELETE 类业务接口）额外校验 `role != 'readonly'`，否则 403。**（后端为最终防线，前端禁用仅为体验）**
- 鉴权失败统一响应 `{ "error": "..." }`：无 token/无效 → 401；readonly 调写接口 → 403；被封禁管理员 → 401。

### 4.2 权限矩阵表

| 功能 / 操作 | super | operator | readonly | 前端表现 | 后端校验 |
|---|:---:|:---:|:---:|---|---|
| 管理员登录 | ✅ | ✅ | ✅ | 登录页 | requireAdmin（含 role 校验） |
| 仪表盘查看 | ✅ | ✅ | ✅ | 可见 | 200 |
| 用户-列表/搜索/分页 | ✅ | ✅ | ✅ | 可见 | 200 |
| 用户-改积分（正负，带原因） | ✅ | ✅ | ❌ | 按钮禁用 | 403 if readonly |
| 用户-封禁/解封（普通用户） | ✅ | ✅ | ❌ | 按钮禁用 | 403 if readonly |
| 用户-改昵称 | ✅ | ✅ | ❌ | 按钮禁用 | 403 if readonly |
| 用户-封禁/解封（其他管理员） | ✅ | ❌ | ❌ | 仅 super 可见入口 | 403 if not super |
| 商品 CRUD | ✅ | ✅ | ❌（仅查看） | 新建/编辑/删除禁用 | 403 if readonly |
| 公告 CRUD | ✅ | ✅ | ❌（仅查看） | 同上 | 403 if readonly |
| 任务 CRUD | ✅ | ✅ | ❌（仅查看） | 同上 | 403 if readonly |
| 关卡 CRUD | ✅ | ✅ | ❌（仅查看） | 同上 | 403 if readonly |
| 系统配置-读取 | ✅ | ✅ | ✅ | 可见 | 200 |
| 系统配置-写入 | ✅ | ✅ | ❌ | 按钮禁用 | 403 if readonly |
| 管理员账户管理（创建/分配角色/禁用） | ✅ | ❌ | ❌（导航隐藏） | 导航项仅 super 显示 | 403 if not super |
| 审计日志查看 | ✅ | ❌（建议） | ❌（导航隐藏） | 导航项仅 super 显示 | 403 if not super |

### 4.3 readonly 的"不可见 vs 禁用"明确清单

- **对 readonly 完全隐藏的导航/页面**（super 专属）：`管理员账户管理`、`审计日志查看`。
- **对 readonly 可见但禁用写按钮**（置灰 `disabled`）：用户管理里的`改积分/封禁/解封/改昵称`、商品/公告/任务/关卡的`新建/编辑/删除`、配置的`保存/写入`。
- **对 readonly 可见且只读**：仪表盘、用户列表、商品/公告/任务/关卡列表（GET）、配置读取页（GET config）。
- **operator 受限点**：无任何"管理员账户管理"入口（既不能创建管理员，也不能改他人角色/禁用其他管理员）；其余内容数据可写。

---

## 5. 审计日志范围（重点）

### 5.1 表结构（新增 `admin_audit_log`）

```sql
CREATE TABLE IF NOT EXISTS admin_audit_log (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  admin_id        TEXT    NOT NULL,   -- 操作人 users.id
  admin_phone     TEXT    NOT NULL,   -- 操作人手机号（冗余，便于追溯）
  admin_role      TEXT    NOT NULL,   -- 操作时角色 super/operator/readonly
  action          TEXT    NOT NULL,   -- 动作类型枚举（见 5.2）
  target_type     TEXT,               -- user/shop_item/notice/task/level/config/admin_account
  target_id       TEXT,               -- 目标对象 id
  before_snapshot TEXT,               -- 变更前摘要（JSON，可空）
  after_snapshot  TEXT,               -- 变更后摘要（JSON，可空）
  source_ip       TEXT,               -- 来源 IP（取 CF-Connecting-IP 头）
  user_agent      TEXT,               -- 浏览器 UA（可空）
  created_at      INTEGER NOT NULL    -- epoch 毫秒
);
CREATE INDEX idx_audit_admin    ON admin_audit_log(admin_id);
CREATE INDEX idx_audit_created  ON admin_audit_log(created_at);
```

### 5.2 必须落审计的操作（action 枚举）

| action | 触发场景 | 记录 before/after |
|---|---|---|
| `LOGIN_SUCCESS` | 管理员登录成功 | 不记快照 |
| `LOGIN_FAILED` | 登录失败（账号/密码/无权限） | 不记快照，记 attempted phone |
| `ADJUST_POINTS` | 改用户积分（正负） | 记改前/改后 points + reason |
| `BAN_USER` / `UNBAN_USER` | 封禁/解封普通用户 | 记 banned 前后 |
| `CREATE_SHOP_ITEM` / `UPDATE_SHOP_ITEM` / `DELETE_SHOP_ITEM` | 商品增删改 | 记关键字段（name/points_price/stock 等）前后 |
| `CREATE_NOTICE` / `UPDATE_NOTICE` / `DELETE_NOTICE` | 公告增删改 | 记 title/type 前后 |
| `CREATE_TASK` / `UPDATE_TASK` / `DELETE_TASK` | 任务增删改 | 记 name/points_reward 前后 |
| `CREATE_LEVEL` / `UPDATE_LEVEL` / `DELETE_LEVEL` | 关卡增删改 | 记 level_id/difficulty；`layout_data` **仅记长度/hash**，避免大字段全量落库 |
| `UPDATE_CONFIG` | 系统配置修改 | 记 key + 前后 value |
| `CREATE_ADMIN` / `UPDATE_ADMIN_ROLE` / `DISABLE_ADMIN` | 管理员账户管理（super 专属） | 记目标 admin id + 角色前后 |

### 5.3 不记录（减少噪声）

- 所有**读操作**（list/get/dashboard/stats/审计日志查看本身）。
- readonly 的所有查看动作。
- 登录页静态资源等非业务请求。

### 5.4 落库时机与一致性

- 在 admin handler 业务逻辑**提交 DB 变更成功后**同步 `INSERT` 一条审计记录（同一事务或紧随其后）。
- `source_ip` 取自 `request.headers.get('CF-Connecting-IP')`，无则取 `request.headers.get('x-forwarded-for')`。
- 审计记录**不可被任何角色修改或删除**（无对应写/删接口），保证可追溯性。

---

## 6. UI 设计要点

### 6.1 通用组件（MUI）

- **列表页**：`Table` + `TablePagination`（默认每页 20，最大 100）+ 顶部 `TextField`（搜索/keyword）+ `Button`（新建，按角色禁用）。行内 `IconButton`（编辑/删除，按角色禁用）。
- **表单/编辑**：`Dialog` + `TextField`/`Select`/`Switch`；关卡 `layout_data` 用多行 `TextField`（`inputProps` 等宽字体）+ JSON 格式校验（解析失败提示，不提交）。
- **危险操作**：`Dialog` 二次确认（如"确认封禁用户 XXX？"）。
- **反馈**：`Snackbar`/`Toast` 统一提示成功/错误；401 自动跳登录。

### 6.2 登录页

- 居中 `Card`：标题"秘境消消乐 · 管理后台" + 手机号/密码 `TextField` + 登录 `Button`；失败 toast。

```
+----------------------------------+
|       秘境消消乐 · 管理后台        |
|  ┌────────────────────────────┐  |
|  │ 手机号: [___________]      │  |
|  │ 密码:   [___________]      │  |
|  │        [   登 录   ]       │  |
|  └────────────────────────────┘  |
+----------------------------------+
```

### 6.3 主框架布局（侧边栏 + 顶栏）

- 左侧 `Drawer`：导航项按角色过滤（super 含"管理员账户管理""审计日志"，operator/readonly 不含）。
- 顶栏 `AppBar`：左侧项目名，右侧显示 `[角色 Chip] [手机号] [退出]`。

```
+-----------------------------------------------------------+
|  AppBar: 秘境消消乐·管理后台   [super|运营|只读] 138*** [退出]│
+----------+------------------------------------------------+
| Drawer   |  Content                                        |
| - 仪表盘 |  ┌─────────────────────────────────────────┐    |
| - 用户   |  │ 标题 + [新建](readonly禁用)  [搜索框]    │    |
| - 商品   |  │ ┌─────────────────────────────────────┐ │    |
| - 公告   |  │ │ MUI Table (分页)                   │ │    |
| - 任务   |  │ │ 行: [编辑][删除](readonly禁用)     │ │    |
| - 关卡   |  │ └─────────────────────────────────────┘ │    |
| - 配置   |  └─────────────────────────────────────────┘    |
| (super)  |  Dialog: 表单编辑 (TextField/Select/Switch)     |
| - 管理员 |                                                 |
| - 审计   |                                                 |
+----------+------------------------------------------------+
```

### 6.4 受保护路由与角色显隐

- `ProtectedRoute`：读取 `AuthStore`（Zustand）token，无则 `<Navigate to="/login">`；Axios 拦截器遇 401 清 token 并跳登录。
- 按钮显隐助手 `useCanWrite()`：返回 `role !== 'readonly'`；页面顶层用 `role === 'super'` 控制"管理员账户管理/审计日志"导航与入口。
- 路由表见 v1.0 §2.2（`App.tsx`）：`/login` 公开，其余包在 `ProtectedRoute` 内。

### 6.5 仪表盘指标（扩展 v1.0 §3.2.8）

`GET /api/admin/stats` 响应建议字段：`users_total`、`today_signup`、`exchange_total`（兑换数）、`points_total`（积分总量）、`notice_count`、`banned_count`、`shop_item_count`、`task_count`、`level_count`。卡片式展示 + （P1）轻量趋势图。

---

## 7. 待确认问题（仅悬而未决，附推荐默认值）

以下为 v1.0 §8 七项经用户确认后的**剩余**待定项，均给出推荐默认值，便于直接拍板：

| # | 问题 | 推荐默认值 |
|---|---|---|
| Q1 | **角色数据模型落地**：用 `role TEXT` 取代 `is_admin`，还是两者并存？ | 以 **`role TEXT`（super/operator/readonly，默认 readonly）取代 `is_admin`**；"是否管理员"由 `role != 'readonly'` 派生，减少冗余列。请架构师据此修订 T01 的 `migrateSchema` 与 `requireAdmin`。**（已与 v1.0 协调，见 §0/§4）** |
| Q2 | **多语言字段维护**：商品/公告/任务含 `name_en/tw/ja/ko`，后台是否维护？ | **本期仅维护默认语言（中文）**，其他语言列留空，由 Android 客户端 fallback 默认语言。多语言 UI 放入 P2-2。 |
| Q3 | **封禁实时拦截游戏端**：`is_banned` 是否在玩家鉴权处生效？ | **二期（P2-1）**。本期仅作数据标记，不做游戏端实时拦截。 |
| Q4 | **登录态持久化**：localStorage / sessionStorage / 记住我？ | **localStorage 默认**（关浏览器仍在）；不做"记住我"选项（管理员设备可信假设）；access 过期静默 refresh（P1-5）。 |
| Q5 | **审计日志查看权限**：是否对 readonly 开放？ | **仅 super**（operator/readonly 隐藏导航）。理由：审计含其他管理员敏感操作，仅最高权限可见。 |
| Q6 | **admin token 有效期 & 刷新**：access/refresh 时长？ | access **2h**、refresh **7d**；前端静默 refresh（P1-5）。JWT payload 仍需带 `role` 以便刷新后仍为原角色。 |
| Q7 | **初始管理员发放**：seed 脚本手机号/密码来源？ | seed 从**环境变量**读取（`ADMIN_PHONE`/`ADMIN_PASSWORD`），避免明文落库；脚本设计为幂等（已存在则更新角色为 super），仅运行一次。 |

**已确认无需再议的项（来自用户决策）**：前端用 Pages 独立站（React+Vite+MUI）调 `/api/admin/*` ✅；复用 `users` 表 + 权限分级 ✅；需审计日志 ✅；seed 一次性初始化 ✅；走免费额度（无自定义域，用 `ADMIN_WEB_ORIGIN` 具体源 CORS）✅。

---

> 交付说明：本文档为**增量 PRD**，技术架构/接口/部署以 v1.0 设计为准；本文补全的业务规则（尤其 §4 角色模型、§5 审计范围）请架构师据此修订 `system_design.md` 后进入开发。
