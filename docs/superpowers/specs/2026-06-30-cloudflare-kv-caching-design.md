# Cloudflare KV 边缘缓存设计方案

## 需求背景
为进一步加快后端 API 接口的响应速度，我们在全接口批处理优化的基础上引入 **Cloudflare KV (Key-Value) 边缘缓存**。
对于读取高频、但数据更新不频繁的只读/静态接口，我们在 CDN 边缘节点进行缓存。请求到达 Worker 后，可直接从最近的边缘 KV 中读取并返回，避免查询 D1 数据库，响应速度可降至 **10-20ms 左右**。

---

## 缓存策略设计

我们计划对以下 4 个高频只读接口实施 KV 缓存策略：

### 1. 商店商品列表接口 (`GET /api/shop/items`)
- **缓存 Key**：`shop_items_${lang}` (区分语言)
- **TTL (缓存有效期)**：10分钟 (`600` 秒)
- **主动失效策略**：当玩家在商店成功兑换道具（调用 `POST /api/shop/exchange`）导致库存发生改变时，在 Worker 中主动删除所有语言的商品列表缓存，确保商品库存数据的实时准确性。

### 2. 系统公告接口 (`GET /api/notice/list`)
- **缓存 Key**：`notices_${lang}` (区分语言)
- **TTL (缓存有效期)**：1小时 (`3600` 秒)
- **更新策略**：公告属于极低频变动数据，1小时 TTL 可最大程度降低数据库负担。

### 3. 游戏排行榜接口 (`GET /api/leaderboard`)
- **缓存 Key**：`leaderboard_${levelId}_${type}_${page}_${limit}` (区分关卡、类型、分页)
- **TTL (缓存有效期)**：30秒 (`30` 秒)
- **说明**：排行榜的排序和连表查询相对繁重，30秒 TTL 既能对并发请求形成强力保护，又对玩家体验几乎无感知。

### 4. 系统配置接口 (`GET /api/admin/config`)
- **缓存 Key**：`admin_config_list`
- **TTL (缓存有效期)**：10分钟 (`600` 秒)
- **主动失效策略**：当管理员修改配置（调用 `POST /api/admin/config`）时，主动删除该 KV 缓存。

---

## 影响文件清单
- **[MODIFY] [wrangler.jsonc](file:///e:/file/sheeps/server/wrangler.jsonc)**: 添加 KV Namespace 绑定 `SHEEPS_CACHE`。
- **[MODIFY] [index.ts](file:///e:/file/sheeps/server/src/index.ts)**:
  - 扩展 `Env` 接口，加入 `SHEEPS_CACHE: KVNamespace`。
  - 在商店列表、兑换、公告、排行榜、配置读写路由中引入 KV 缓存查询与主动失效/更新逻辑。

---

## 验证计划
1. **自动部署验证**：
   - 运行 `npx wrangler deploy`，验证编译并部署无误。
2. **性能与一致性校验**：
   - 请求 `/api/shop/items` 观察第二次请求的时延是否降至 ~20ms（从 KV 命中）。
   - 进行一次购买，校验后续请求的库存是否正确发生扣减。
