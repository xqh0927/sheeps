# 管理后台用户角色分级设计方案（PRD 风格）

> 项目：sheeps（秘境消消乐）· 管理后台 admin-console
> 作者：许清楚（产品经理）
> 日期：2025-07-10
> 范围：角色定义 + 跨模块权限矩阵 + 用户故事 + 待确认问题 + 现有守卫现状
> 说明：本文档只做设计，不含实现代码。所有结论均基于真实代码探查（`admin-console/src`、`server/src/handlers/admin.ts`、`server/src/helpers.ts`、`server/src/auth-utils.ts`）。

---

## 0. 核心结论速览

- **角色固定 4 级**：`user`（普通玩家，无后台权限） / `readonly`（只读） / `operator`（运营） / `super`（超级管理员）。
- **`users` 单表共用**：Android 注册玩家与后台管理员同表，仅靠 `role` 列区分。普通玩家 `role='user'` 进入后台应"什么都没有"，登录即拦截。
- **后端现状：普通玩家已被天然拦截**——`adminLogin` 仅放行 `super/operator`，`requireAdmin` 也排除 `user`。真正的缺口在 **readonly 无法登录**、**清空缓存无写守卫**、**前端无角色级兜底**。详见第 5 节。
- **系统配置（含游戏模式开关、清空 KV 缓存、任意配置键写入）建议仅 `super` 可编辑**，运营仅查看。风险说明见第 3 节注释与第 4 节。

---

## 1. 现有后台功能模块枚举（真实探查）

通过 `admin-console/src/pages/` 与 `server/src/handlers/admin.ts` 路由逐文件核对，后台实际存在以下 **13 个功能模块**（不含 Login 登录页）：

| # | 模块（页面文件） | 后端主要接口 | 业务含义 |
|---|----------------|-------------|---------|
| 1 | 数据概览 `Dashboard.tsx` | `GET /api/admin/stats` | 用户/内容/经济/无尽模式统计看板（只读） |
| 2 | 用户管理 `Users.tsx` | `users` 列表 + 积分/封禁/昵称/背包/删除 | 玩家账户运营操作 |
| 3 | 卡片皮肤管理 `SkinProducts.tsx` | `shop_items`(SKIN_) + `skin_tiles` 卡面 | 皮肤商品与 12 张卡面 |
| 4 | 道具管理 `PropProducts.tsx` | `shop_items`(非SKIN_) + `item_icons` 图标 | 法宝道具商品与图标 |
| 5 | 公告管理 `Notices.tsx` | `notice` CRUD | 活动/更新/维护公告 |
| 6 | 任务管理 `Tasks.tsx` | `task` CRUD | 每日/成就任务 |
| 7 | 关卡管理 `Levels.tsx` | `levels` + `level_tiles` | 关卡布局与难度 |
| 8 | App 版本管理 `AppVersions.tsx` | `app_version` CRUD + APK 上传 | 版本发布/强更 |
| 9 | 多语言管理 `I18nManager.tsx` | `i18n_strings` CRUD | 5 语种文案 |
| 10 | 排行榜管理 `LeaderboardManager.tsx` | `leaderboard` 列表/补录/改分/删 | 关卡榜/无尽榜 |
| 11 | 系统配置 `Config.tsx` + `GameModeSwitch.tsx` | `config` 读写 + 游戏模式开关 + 清空缓存 | 解锁门槛/签到/模式开关/KV |
| 12 | 管理员账户管理 `Accounts.tsx` | `accounts` 列表/创建/改角色/禁用 | 后台账号生命周期（**super 专属**） |
| 13 | 操作审计日志 `AuditLogs.tsx` | `audit-logs` 列表 | 写操作留痕（**super 专属**） |

---

## 2. 角色体系定义（4 级）

| 角色 | 取值 | 适用人群 | 职责边界 |
|------|------|---------|---------|
| 普通用户 | `user` | Android 端注册玩家 | **无任何后台权限**，登录即被拦截/重定向到无权限提示。后台是给运营/管理员用的，不是给玩家用的。 |
| 只读 | `readonly` | 审计员、盯盘负责人、外包观察员 | 仅查看各模块数据与统计看板，**所有写入按钮禁用**，不能增删改，不能查看审计日志（默认）。 |
| 运营 | `operator` | 日常运营同学 | 用户管理（封禁/积分/昵称/背包）、商品上下架与编辑、公告/任务发布、版本发布、多语言编辑、排行榜补录改分等**业务操作**；**不能**管理其他后台账户（不能新增/禁用管理员、不能改他人角色），**不能**改系统级敏感配置（游戏模式开关、清空缓存、任意配置键）。 |
| 超级管理员 | `super` | 技术负责人 / 主理人 | 全部权限，含管理其他后台账户（新增/改角色/禁用）、系统配置、清空缓存、查看审计日志。 |

> 设计取舍：默认推荐 **4 级**，不过度拆"财务/客服"等独立角色（避免 admin 账户膨胀与《网络安全法》实名关联复杂度）。如未来客服需独立处理玩家工单，可再评估增设，不在本期范围。

---

## 3. 跨模块权限矩阵（角色 × 模块）

图例：**无** = 不可见/不可访问 · **查看** = 只读 · **编辑** = 增删改业务数据 · **管理** = 含账号/系统/审计等最高权限。

| 模块 | 普通用户 `user` | 只读 `readonly` | 运营 `operator` | 超级管理员 `super` |
|------|:---:|:---:|:---:|:---:|
| 1. 数据概览 Dashboard | 无 | 查看 | 查看 | 查看 |
| 2. 用户管理 Users | 无 | 查看 | 编辑（封禁/积分/昵称/背包） | 管理（含删除用户） |
| 3. 卡片皮肤管理 SkinProducts | 无 | 查看 | 编辑 | 管理 |
| 4. 道具管理 PropProducts | 无 | 查看 | 编辑 | 管理 |
| 5. 公告管理 Notices | 无 | 查看 | 编辑 | 管理 |
| 6. 任务管理 Tasks | 无 | 查看 | 编辑 | 管理 |
| 7. 关卡管理 Levels | 无 | 查看 | 编辑 | 管理 |
| 8. App 版本管理 AppVersions | 无 | 查看 | 编辑（含发布/APK上传） | 管理 |
| 9. 多语言管理 I18nManager | 无 | 查看 | 编辑 | 管理 |
| 10. 排行榜管理 LeaderboardManager | 无 | 查看 | 编辑（补录/改分/删） | 管理 |
| 11. 系统配置 Config | 无 | 查看 | 查看（建议）⚠️ | 管理（含游戏模式/缓存） |
| 12. 管理员账户管理 Accounts | 无 | 无 | 无 | 管理 |
| 13. 操作审计日志 AuditLogs | 无 | 无 | 无 | 查看/管理 |

### 特别标注（硬约束）

- **普通用户 `user`：所有模块 = 无**。登录即拦截（后端 `adminLogin` 当前已拒绝 `user`；并建议前端加角色兜底重定向到无权限页）。
- **管理员账户管理（新增/改角色/禁用）：仅 `super`**。运营与只读均 = 无。
- **系统配置 / 游戏模式开关：默认仅 `super` 可编辑**（管理）；运营建议仅"查看"。
  - ⚠️ **风险说明（若放开给 operator）**：游戏模式开关（无尽/对战）直接决定线上流量分配与收入结构；`config` 表的"新增配置"可写入任意 key（含潜在支付/密钥类配置），风险极高；"清空 KV 缓存"会击穿缓存导致全量回源 D1，可能引发线上抖动。故建议 super-only。若坚持让运营可改**非敏感**配置，必须显式维护一份"安全 key 白名单"，而非放开整表。

### 细分与例外（在模块内进一步收敛的敏感动作）

| 敏感子动作 | 当前后端守卫 | 设计建议 |
|-----------|-------------|---------|
| 删除用户（级联删玩家全部数据） | `assertCanWrite`（operator 也可执行） | **改 `super` only**（破坏性极强，误删不可恢复） |
| 游戏模式开关 `gamemode_*` | `assertCanWrite`（operator 可改） | **改 `super` only** |
| 清空 KV 缓存 `/cache/clear` | **无任何守卫**（仅 requireAdmin） | **加 `assertSuper`**（即使 readonly 也能清空，属高危缺口） |
| 新增/删除任意配置 key | `assertCanWrite`（operator 可写） | **改 `super` only** |
| 关卡布局 `layout_data` 编辑 | `assertCanWrite` | 保留 operator 编辑（属常规运营内容） |

---

## 4. 用户故事

1. **作为运营**，我希望能封禁违规玩家、调整其积分/昵称/背包，但**不能删除玩家账户、不能更改任何人的角色**，以免误操作或越权造成不可恢复后果。
2. **作为超级管理员**，我希望能创建/禁用其他后台账户并调整其角色，确保后台账号生命周期可控；同时系统应在"降级/禁用最后一个 super"时给出保护，避免后台被锁死。
3. **作为只读角色（审计/盯盘）**，我希望能查看所有业务模块数据与统计看板，但**任何写入按钮都应是禁用态**，从 UI 上杜绝误操作影响线上。
4. **作为普通玩家**，误点进管理后台登录页时，应被**明确告知"无后台权限"并阻止进入**，而不是看到空白后台、报错堆栈或"账号不存在"的混淆提示。
5. **作为运营**，我希望能发布 App 版本、发布公告/任务、上下架商品来驱动日常活动；但游戏模式开关、清空缓存这类会影响全服的敏感操作，应由 super 把关。

---

## 5. 现有权限守卫现状（代码级探查报告）

### 5.1 后端守卫（`server/src/helpers.ts` + `server/src/handlers/admin.ts`）

| 守卫函数 | 逻辑 | 现状评价 |
|---------|------|---------|
| `requireAdmin` (`helpers.ts:74`) | `type==='access' && role IN (super,operator,readonly) && is_banned=0` | ✅ 已**天然排除 `user`**，普通玩家拿不到任何后台接口。 |
| `assertCanWrite` (`helpers.ts:100`) | `readonly → 403` | ✅ 只读写操作被拦。 |
| `assertSuper` (`helpers.ts:111`) | `非 super → 403` | ✅ 账户管理/审计日志已加此守卫。 |
| `adminLogin` (`admin.ts:93`) | **仅放行 `['super','operator']`** | ⚠️ **缺口：readonly 无法登录**——`readonly` 在 `ROLE_OPTIONS`/`ADMIN_ROLES` 中定义，却进不了后台，等于废角色。需把 `readonly` 加入放行白名单。 |
| 写接口（用户/商品/公告/任务/关卡/版本/i18n/排行榜） | 均 `assertCanWrite` | ✅ 符合"运营可写、只读拦"。 |
| `updateConfig`（含游戏模式开关） | 仅 `assertCanWrite` | ⚠️ operator 也能改游戏模式/任意 key，与设计建议（super-only）不符。 |
| `/api/admin/cache/clear` (`admin.ts:1257`) | **无任何写守卫**（仅过 requireAdmin） | 🔴 **高危缺口：readonly/operator 均可清空全站 KV 缓存**，需加 `assertSuper`。 |
| `deleteUser` (`admin.ts:200`) | 仅 `assertCanWrite` + 防删管理员/防删自己 | ⚠️ operator 可级联删玩家全量数据，建议收敛为 super-only。 |
| 审计日志 `audit-logs` | `assertSuper` + 前端告警 | ✅ super-only，符合。 |

### 5.2 前端守卫（`admin-console/src`）

| 文件 | 逻辑 | 现状评价 |
|------|------|---------|
| `store/auth.ts` | `AdminRole = 'super'|'operator'|'readonly'`（**不含 `user'`**）；`canWrite() = role !== 'readonly'` | ⚠️ 类型漏了 `user`；且 `canWrite()` 对 `user` 会返回 `true`（潜在漏洞，依赖后端兜底）。 |
| `components/ProtectedRoute.tsx` | 仅校验 `token && user`，**不校验角色** | ⚠️ 无角色兜底；当前靠后端拦截 user，但应加 `role ∈ 后台角色集` 判断，否则越权页面先渲染再报错。 |
| `components/Layout.tsx` | 仅 `superOnly` 项（账户/审计）对非 super 隐藏 | ✅ 菜单已按 super 收敛；其余模块对 operator/readonly 可见（readonly 靠 `canWrite=false` 禁按钮）。 |
| `pages/Accounts.tsx`、`AuditLogs.tsx` | 非 super 显示"仅超级管理员可访问"告警 | ✅ 已有前端兜底。 |
| `pages/Login.tsx` | 登录失败直接 `extractError(err)` | ⚠️ 玩家与"账号不存在"共用 401 文案，需区分"无后台权限"提示。 |

### 5.3 现状小结

- **普通玩家已被后端完全挡在门外**（登录拒绝 + requireAdmin 排除 user），"登录即无权限"的硬约束在 API 层成立。
- **主要待修补项**（实现侧）：① `adminLogin` 放行 `readonly`；② `/cache/clear` 加 `assertSuper`；③ `deleteUser` 与 `updateConfig`/游戏模式开关收敛为 super-only（按第 3 节建议）；④ 前端 `ProtectedRoute` 增加角色兜底 + 独立"无权限"页；⑤ 登录失败文案区分玩家。

---

## 6. 待用户确认的问题（Open Questions）

1. **运营能否编辑系统配置？** 本文建议"运营仅查看，游戏模式开关/清空缓存/任意配置键 = super-only"。若希望运营可改**非敏感**配置，请明确"安全 key 白名单"范围。
2. **"删除用户"（级联删玩家全部数据）归谁？** 建议 super-only（当前 operator 也可执行）。是否同意？
3. **普通用户登录后台的表现？** 跳登录页并提示"无后台权限"，还是单独的无权限页？另外当前后端把玩家与"账号不存在"合并为同一 401 文案，是否需区分？
4. **只读角色是否需要查看审计日志？** 当前审计日志仅 super 可见。若 readonly 是"审计/盯盘"用途，可能需授予"只读查看审计"（不含导出/删除）。
5. **readonly 当前无法登录**（adminLogin 仅放行 super/operator），是否符合预期？设计上 readonly 需能登录查看，需研发修复放行。
6. **是否需要审计日志导出 / 留存期限策略？** 目前仅落库 `admin_audit_log`，无限增长、无导出。
7. **"最后一个 super 降级/禁用"的锁死防护？** 代码注释提到但未强制防最后一个 super 被降级导致无人能管理，是否要加固？

---

## 7. 实现侧待办（供研发，非本次设计范围）

- [ ] `adminLogin`：放行白名单加入 `'readonly'`。
- [ ] `/api/admin/cache/clear`：增加 `assertSuper` 守卫。
- [ ] `deleteUser` / `updateConfig`（游戏模式开关、任意 key 写入）：改为 `assertSuper`（或按白名单细分）。
- [ ] 前端 `ProtectedRoute`：增加 `role ∈ {super,operator,readonly}` 兜底，否则跳转无权限页。
- [ ] 新增"无权限"提示页，普通玩家登录失败后明确引导。
- [ ] `store/auth.ts`：`AdminRole` 补充 `'user'` 分支，`canWrite()`/`isSuper()` 对 `user` 返回 false 兜底。
- [ ] 登录失败文案区分"普通玩家无权限"与"账号/密码错误"。
