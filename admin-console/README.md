# 秘境消消乐 · 管理后台（Admin Console）

基于 React 18 + Vite 5 + MUI 5 + React Router 6 + Zustand + Axios 的 Cloudflare Pages 管理后台，
对接现有 Cloudflare Worker 的 `/api/admin/*` 接口，实现运营概览、用户与背包、皮肤商品 / 道具商品、
公告、任务、关卡与关卡体检（LevelHealth）、App 版本、国际化文案（i18n）、排行榜、系统配置管理，
以及三级角色权限（super / operator / readonly）与操作审计日志。

## 本地开发

```bash
cd admin-console
npm install
# 配置后端地址（Worker 域名或本地 wrangler dev 地址）
echo "VITE_API_BASE=http://127.0.0.1:8787" > .env
npm run dev
```

打开 http://localhost:5173 ，使用 seed 脚本创建的超级管理员账号登录。

## 构建

```bash
npm run build      # 类型检查并打包，产物输出到 dist/
npm run preview    # 本地预览构建结果
npm run typecheck  # 仅类型检查
```

## 部署（Cloudflare Pages）

1. **快捷脚本部署（推荐）**：在仓库根目录执行 `npm run deploy-admin`（调用 [deploy-admin.js](../deploy-admin.js)），自动安装依赖、构建并发布到 Cloudflare Pages 项目 `miadmin-console`。
2. **手动 Wrangler 部署**：
   ```bash
   cd admin-console
   npm run build
   npx wrangler pages deploy dist --project-name=miadmin-console --branch=main
   ```
3. **Cloudflare Pages Git 集成**：在 Pages 控制台连接本仓库，构建目录填 `admin-console/dist`，构建命令填 `cd admin-console && npm run build`，并配置环境变量 `VITE_API_BASE` = 线上 Worker 地址。

## 重置账号密码

在 `server/` 目录运行 `npx wrangler secret put ACCOUNT_RESET_DEFAULT_PASSWORD`，按提示输入默认密码（6–20 位，至少含一个英文字母和一个数字）。本地 Wrangler 可在 `.dev.vars` 配置同名变量。

超级管理员在「用户管理」中点击目标账号的「重置为默认密码」，核对手机号、昵称和角色后确认。支持玩家和各级管理员的手机号登录账号。审计日志可按「重置账号密码」检索。此次操作替换登录密码，已有登录会话继续按原有效期管理。

## 权限模型

| 角色 | 能力 |
|------|------|
| super | 全部读写 + 管理员账户管理 + 审计日志查看 + KV 缓存清理 |
| operator | 业务数据读写（用户/皮肤与道具商品/公告/任务/关卡/版本/国际化/排行榜/配置） |
| readonly | 仅查看，写按钮禁用；后端 403 为最终防线 |

> 前端禁用仅为体验，真实权限由后端 `requireAdmin` / `assertCanWrite` / `assertSuper` 强制保证。

## 后端配套（已在 `server/` 实现）

- `server/src/handlers/admin.ts`：全部后台接口 + 三级角色守卫 + 审计落库
- `server/schema.sql` & `server/migrations/`：D1 建表与增量迁移脚本（首请求自动迁移已移除，变更表结构通过 `npm run deploy` 执行 `wrangler d1 migrations apply`）
- `server/scripts/seed-admin.mjs`：幂等创建首个 super 管理员
- 部署：`wrangler secret put JWT_SECRET / AES_KEY_HEX / ADMIN_WEB_ORIGIN`；`ADMIN_PHONE`/`ADMIN_PASSWORD` 跑 seed；后端部署可在根目录执行 `npm run deploy-backend` 或在 `server/` 下执行 `npm run deploy`。

详见 `docs/system_design.md` 与 `docs/admin-prd.md`。

## 浏览器回归

在本目录执行 `npm ci` 和 `npx playwright install chromium`，然后用 `npm run test:ui` 验证切换账号后的背包状态、排行榜记录归属与本地日期筛选。设置 `CHROME_BIN` 可以使用已安装的 Chrome。测试会启动本地 Vite 服务并使用独立的浏览器会话与测试数据。
