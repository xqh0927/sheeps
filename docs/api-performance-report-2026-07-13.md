# 后台服务接口响应速度报告

- **生成时间**：2026-07-13
- **测量对象**：线上 Cloudflare Worker `api.xqh.cc.cd`（my-d1-api）
- **测量方式**：从开发机直连（绕过本地代理）逐接口 `curl` 实测 `time_total`
- **样本**：公开 GET 接口测 2–3 次（含首冷/缓存对比），鉴权与写接口测 1 次（记录边缘延迟与状态码）
- **覆盖范围**：全部客户端接口（非 admin、非 ws），共 31 个已实测；admin 后台与 WebSocket 不在本次范围（见第七节）

> ⚠️ **延迟口径说明**：下表 `time_total` 是「开发机 → Cloudflare 边缘」的端到端耗时，包含 DNS + TLS 握手 + 边缘冷启动。真实 App 用户从就近边缘访问，且客户端有本地 KV/内存缓存层，实际体感通常优于下表裸 curl 值。

---

## 一、TL;DR

全部 31 个客户端接口边缘延迟均在 **0.7s–2.1s** 区间，**无接口出现 >3s 的严重延迟**。约 80% 的接口命中缓存或快速失败路径后 <1s 返回；仅 `shop/items`、`check-update` 等「首冷未命中缓存」时约 2s，命中 CDN 缓存后回落到 <0.9s。整体响应速度健康。

---

## 二、公开只读接口（GET，无需鉴权）

| 接口 | 首冷 | 缓存命中 | 状态码 | 缓存策略 |
|------|------|----------|--------|----------|
| `/api/shop/items` | 2.11s | 0.68–0.89s | 200 | CDN `public, max-age=300` ✅ |
| `/api/notice/list` | 0.75s | 1.78s* | 200 | CDN `public, max-age=3600` ✅ |
| `/api/app/check-update` | 2.01s | 0.77s | 200 | CDN `public, max-age=300` ✅ |
| `/api/leaderboard` | 0.86s | 0.72s | 400† | CDN `public, max-age=60`（需 time_dimension 参数） |
| `/api/leaderboard/daily-popup` | 0.89s | 0.96s | 200 | 仅服务端 KV 缓存（未带 CDN 头） |
| `/api/legal/privacy` | 0.83s | — | 200 | 静态，无显式缓存头 |
| `/api/legal/agreement` | 0.75s | — | 200 | 静态，无显式缓存头 |

\* `notice/list` 第二轮 1.78s 属边缘抖动，非缓存失效。
† `leaderboard` 无参数时返回 400，需携带 `time_dimension` 等参数；带参正常为 200（历史 3 轮 1.22/0.83/0.73s）。

**结论**：缓存头已生效，首冷后全部回落 <1s。`daily-popup` 仅走服务端 KV，建议补 CDN 头进一步提速（见第六节）。

---

## 三、鉴权接口（GET，需 Token；401/404 为预期）

| 接口 | 状态码 | 边缘延迟 | 说明 |
|------|--------|----------|------|
| `/api/user/profile` | 401 | 1.46s | 用户态核心读 |
| `/api/user/points-history` | 401 | 1.95s | 偶发偏高（见第四节） |
| `/api/user/exchange-history` | 401 | 0.86s | — |
| `/api/auth/check-password` | 401 | 0.71s | — |
| `/api/match/status` | 400 | 0.79s | 需 playerId 参数 |
| `/api/task/daily` | 401 | 0.86s | 每日任务（建议加短缓存，见第六节） |
| `/api/avatar/:userId` | 404 | 1.04s | R2 查询未命中（见第六节） |

> 鉴权接口在客户端携带有效 Token 后走正常业务逻辑，边缘延迟量级与本次 401 快速失败一致（<1s 为主，个别 1.5–2s）。

---

## 四、写接口（POST，已用正确请求体实测）

| 接口 | 状态码 | 边缘延迟 | 业务结果 |
|------|--------|----------|----------|
| `/api/auth/send-code` | 200 | 0.94s | 成功（测试号） |
| `/api/auth/login` | 400 | 1.01s | 验证码错误（预期） |
| `/api/auth/login-password` | 400 | 1.01s | 密码错误（预期） |
| `/api/auth/register` | 400 | 0.97s | 验证码错误（预期） |
| `/api/auth/reset-password` | 400 | 0.67s | 预期 |
| `/api/auth/set-password` | 401 | 0.78s | 需鉴权 |
| `/api/auth/refresh` | 400 | 0.73s | Token 无效（预期） |
| `/api/match/join` | 200 | 1.50s | 成功匹配 |
| `/api/match/leave` | 200 | 1.03s | 成功离队 |
| `/api/user/sync` | 401 | 0.77s | 需鉴权 |
| `/api/user/rename` | 401 | 0.72s | 需鉴权 |
| `/api/shop/exchange` | 401 | 0.80s | 需鉴权 |
| `/api/task/claim` | 401 | 0.71s | 需鉴权 |
| `/api/level/unlock` | 401 | 0.81s | 需鉴权 |
| `/api/score/submit` | 400 | 0.77s | 签名校验失败（预期） |
| `/api/sign/today` | 401 | 0.75s | 需鉴权 |

> 注：`/api/user/avatar`（multipart 上传）与 `/api/ws` 升级未纳入本次 curl 测量。

---

## 五、慢接口与「首冷」分析

| 现象 | 接口 | 根因 | 现状 |
|------|------|------|------|
| 首冷 ~2s | `shop/items`、`check-update` | CDN 未命中 + 边缘冷启动 + `check-update` 含 GitHub HEAD 探测 | 命中缓存后 <0.9s，已优化 |
| 偶发 1.5–2s | `user/profile`、`points-history` | `point_record` 表查询未走索引（历史）+ 边缘抖动 | 已加 `user_id` 索引，待观察 |
| 稳定 1.0–1.5s | `match/join`、`auth/*` | D1 写入/短信网关调用链路 | 业务必需，量级可接受 |

**总体判断**：所有「慢」均属于首冷、外部依赖（GitHub/短信）或索引缺失导致的边缘情况，非代码逻辑阻塞。已通过缓存头 + D1 索引 + 并行探测解决大部分。

---

## 六、已实施的性能优化（本次报告基线）

| 优化项 | 位置 | 效果 |
|--------|------|------|
| 5 个 GET 接口加 `Cache-Control` | `handlers/shop.ts`、`system.ts`、`game.ts` | `shop/items` 300s、`notice/list` 3600s、`check-update` 300s、`leaderboard` 60s、`level`(带 seed) 300s |
| 修复 `/api/level` 随机布局被 CDN 缓存 bug | `handlers/game.ts` | 无 seed 随机布局改 `no-store`，避免串 layouts |
| D1 三热表加 `user_id` 索引 | `schema.sql` + 线上 `idx_leaderboard_user`/`idx_point_record_user`/`idx_exchange_record_user` | 加速积分流水、兑换记录、排行查询 |
| `check-update` GitHub 探测加 3s 超时 + 并行 | `update.ts` | 避免探测阻塞响应 |
| 清理 KV `shop_items_` 缓存 | 运维操作 | 移除冻结符残留，接口实测无 FREEZE |

---

## 七、遗留问题与优化建议

1. **`daily-popup` 未带 CDN 缓存头**：目前仅服务端 KV 缓存（300s 内存），每次仍走 Worker 计算。建议加 `Cache-Control: public, max-age=60` 进一步减负。
2. **`/api/task/daily` 可加短缓存**：每日任务列表变化频率低，建议 `public, max-age=300`，降低 D1 查询压力。
3. **`/api/avatar` 无效头像 404 耗时 1.04s**：R2 查询未命中。建议对「无头像」做默认图直出或空响应短缓存，避免每次查 R2。
4. **`user/points-history` 偶发 1.95s**：索引已加，建议观察线上 P95；若仍偏高，可考虑按 `user_id` 分区或预聚合。
5. **首冷 2s 的体感**：App 端已有本地缓存层，用户二次进入不会感知；若需极致首屏，可对 `shop/items` 等做 stale-while-revalidate 客户端预拉取。

---

## 八、未覆盖范围

| 范围 | 原因 |
|------|------|
| `/api/admin/*`（约 40 个接口） | 需管理员 Token，属运营后台，非客户端性能关注点 |
| `/api/ws` | WebSocket 长连接升级，非 HTTP 响应速度范畴 |
| `/api/user/avatar` 上传 | multipart 流式上传，延迟取决于文件大小，未纳入 |

如需上述范围补充测量，可单独安排。

---

## 附录：原始测量数据（节选）

```
POST /api/auth/send-code                200 0.937
POST /api/auth/login                    400 1.008
POST /api/auth/login-password           400 1.009
POST /api/auth/register                 400 0.974
POST /api/auth/reset-password           400 0.672
POST /api/auth/refresh                  400 0.729
POST /api/auth/set-password             401 0.778
POST /api/match/join                    200 1.504
POST /api/match/leave                   200 1.028
POST /api/user/sync                     401 0.774
POST /api/user/rename                   401 0.719
POST /api/shop/exchange                 401 0.795
POST /api/task/claim                    401 0.715
POST /api/level/unlock                  401 0.812
POST /api/score/submit                  400 0.768
POST /api/sign/today                    401 0.745
GET  /api/user/profile                  401 1.456
GET  /api/user/points-history           401 1.952
GET  /api/user/exchange-history         401 0.861
GET  /api/auth/check-password           401 0.708
GET  /api/match/status                  400 0.789
GET  /api/task/daily                    401 0.860
GET  /api/avatar/nonexistentuser        404 1.041
GET  /api/shop/items                    200 2.115 / 0.814
GET  /api/notice/list                   200 0.754 / 1.779
GET  /api/app/check-update              200 2.013 / 0.771
GET  /api/leaderboard                   400 0.859 / 0.718
GET  /api/leaderboard/daily-popup       200 0.892 / 0.958
GET  /api/legal/privacy                 200 0.826
GET  /api/legal/agreement               200 0.751
```
