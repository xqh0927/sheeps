# 发版说明 · v1.0.0（首次发版）

> 发布日期：2026-07-07
> 范围：管理后台前端（admin-console）+ 后端管理 API（server）+ 发版工具链（release.js）
> 版本号默认 v1.0.0（首版）；如实际版本号不同请按真实值替换。

---

## 一、版本概览

| 模块 | 版本 | 说明 |
|------|------|------|
| 管理后台前端 `admin-console` | v1.0.0 | 首次发布 |
| 后端服务 `server`（Cloudflare Worker） | 随本次发布 | 上线管理 API + 安全基座 |
| 发版脚本 `release.js` | 工具链升级 | 公告发布改走管理后台 API |

---

## 二、新增功能

### 1. 管理后台前端（admin-console）— 首次发布
- 完整后台：登录、布局与权限守卫、仪表盘、用户管理、商店道具、公告管理、任务、关卡、配置、账户、审计日志等 9+ 页面。
- 三级角色权限模型：`super` / `operator` / `readonly`，按角色显隐导航。
- 审计日志表 `admin_audit_log`（操作留痕）。
- 全新 favicon（深靛蓝底 + 白色小羊头像），替换原 Vite 默认图标。

### 2. 后端管理 API（server）
- 管理员登录 `POST /api/admin/login` —— 返回 JWT（有效期 2h）。
- 公告发布 `POST /api/admin/notices` —— 支持多语言（`title/content` 的 `_tw/_en/_ja/_ko`）。
- 用户 / 商店道具 / 任务 / 关卡 / 配置 等管理端点。
- 安全基座：JWT 鉴权、CORS 精确源校验、角色权限校验（`requireAdmin` / `assertCanWrite` / `assertSuper`）。

### 3. 发版工具链（release.js）
- 公告发布改为调用管理后台 API（不再用 `wrangler` 直写 D1/KV）。
- 新增配置：`ADMIN_API_BASE` / `ADMIN_PHONE` / `ADMIN_PASSWORD`（支持环境变量覆盖）。
- 增加凭据守卫：未配置凭据时快速失败并提示。

---

## 三、缺陷修复

- 修复 `server/src/helpers.ts` 中 `translateErrorMessage` 重复导出，导致 `wrangler deploy` 构建失败（`Multiple exports with the same name`）。删除了遗留占位桩，保留基于 `ERROR_TRANSLATIONS` 的多语言实现。

---

## 四、关键变更与注意事项

- **KV 缓存键修复**：`notices_${lang}` → `notices_${lang}_v2`，使客户端公告即时生效（原写法与读端对不上，公告最多延迟 1h 才可见）。
- **版本号（app_version）**：后端暂无写接口，仍由 `release.js` 用 `wrangler` 直写 D1。
- **前端构建体积**：主包约 544 kB（gzip 174 kB），有 chunk 体积提示（非错误，可后续用路由级懒加载 + `manualChunks` 优化）。

---

## 五、部署 / 发布步骤

### 后端（Cloudflare Worker）
1. 注入密钥：`wrangler secret put JWT_SECRET` / `AES_KEY_HEX` / `ADMIN_WEB_ORIGIN`
2. 初始化首个 super 管理员：`node server/scripts/seed-admin.mjs`
3. 部署：`cd server && npx wrangler deploy`

### 前端（Cloudflare Pages）
1. 配置 GitHub Secrets：`VITE_API_BASE` / `CLOUDFLARE_API_TOKEN` / `CLOUDFLARE_ACCOUNT_ID`
2. 构建并部署：`npm run build` 后发布 `dist/`（或走仓库内 Pages 部署工作流）

### App 发版（release.js）
1. 设置环境变量 `ADMIN_PHONE` / `ADMIN_PASSWORD`（或替换文件内占位常量 `REPLACE_WITH_ADMIN_PHONE` / `REPLACE_WITH_ADMIN_PASSWORD`）
2. 在 `main` 分支、wrangler 已登录、git remote 可达状态下运行 `node release.js`

---

## 六、已知问题 / 待办

- 本环境未做真实端到端联调（无 admin 服务 / wrangler 登录态 / git remote），建议首次上线前完整回归一次。
- 前端主包体积告警可后续优化（路由级懒加载 + `manualChunks`）。
- 版本发布接口（`app_version` 写）后续可补为走 API，进一步去除 `release.js` 对 `wrangler` 的依赖。

---

*生成说明：本发版说明由本次会话中已确认的三项改动汇总而成——release.js 工具链升级、helpers.ts 重复导出修复、admin-console 前端落地与 favicon 更换。*
