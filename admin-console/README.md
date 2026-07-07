# 秘境消消乐 · 管理后台（Admin Console）

基于 React 18 + Vite 5 + MUI 5 + React Router 6 + Zustand + Axios 的 Cloudflare Pages 管理后台，
对接现有 Cloudflare Worker 的 `/api/admin/*` 接口，实现用户 / 商品 / 公告 / 任务 / 关卡 / 配置管理，
以及三级角色权限（super / operator / readonly）、操作审计日志。

## 本地开发

```bash
cd admin-console
npm install
# 配置后端地址（Worker 域名）
echo "VITE_API_BASE=https://your-worker.your-subdomain.workers.dev" > .env
npm run dev
```

打开 http://localhost:5173 ，使用 seed 脚本创建的超级管理员账号登录。

## 构建

```bash
npm run build      # 产物输出到 dist/
npm run preview    # 本地预览构建结果
npm run typecheck  # 仅类型检查
```

## 部署（Cloudflare Pages）

1. 在 Cloudflare Pages 创建项目 `miadmin-console`，连接本仓库，构建目录 `admin-console/dist`，
   构建命令 `cd admin-console && npm run build`，环境变量 `VITE_API_BASE` = Worker 地址。
2. 或推送 `main` 分支（改动 `admin-console/**`）由 `.github/workflows/deploy-pages.yml` 自动发布，
   需在仓库 Secrets 配置：`VITE_API_BASE`、`CLOUDFLARE_API_TOKEN`、`CLOUDFLARE_ACCOUNT_ID`。

## 权限模型

| 角色 | 能力 |
|------|------|
| super | 全部读写 + 管理员账户管理 + 审计日志查看 |
| operator | 业务数据读写（用户/商品/公告/任务/关卡/配置） |
| readonly | 仅查看，写按钮禁用；后端 403 为最终防线 |

> 前端禁用仅为体验，真实权限由后端 `requireAdmin` / `assertCanWrite` / `assertSuper` 强制保证。

## 后端配套（已在 `server/` 实现）

- `server/src/handlers/admin.ts`：全部后台接口 + 三级角色守卫 + 审计落库
- `server/schema.sql`：`users` 增加 `role` / `is_banned`，新增 `admin_audit_log` 表
- `server/scripts/seed-admin.mjs`：幂等创建首个 super 管理员
- 部署：`wrangler secret put JWT_SECRET / AES_KEY_HEX / ADMIN_WEB_ORIGIN`；`migrateSchema` 首请求自动加列建表；`ADMIN_PHONE`/`ADMIN_PASSWORD` 跑 seed。

详见 `docs/system_design.md` 与 `docs/admin-prd.md`。
