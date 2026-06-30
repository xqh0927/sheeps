# 后端服务代码拆分与模块化设计方案

## 需求背景
目前后端所有的逻辑都堆积在单一文件 `server/src/index.ts` 中，代码行数已超 1800 行。
由于包含了密码学 JWT 验证、关卡牌型生成算法、版本更新检测、WebSocket 对决长连接轮询、CORS 响应及全部 16 个 API 的路由逻辑，该文件职责过于繁重，可读性和可维护性较差。
为了提高代码质量，方便团队后续的开发与维护，我们计划将代码按照**职责分离（Separation of Concerns）**的原则进行模块化重构与拆分。

---

## 模块化架构设计

拆分后的目录结构如下：

```
server/src/
├── types.ts          # 存放全局共享的数据类型与接口 (Env, User, TileData 等)
├── crypto.ts         # 密码学与安全工具 (JWT 签发校验、SHA-256 签名)
├── helpers.ts        # 常规路由辅助工具 (CORS 头配置、Auth 认证拦截器、语言选择)
├── level.ts          # 关卡生成算法核心逻辑 (Solvable Level 生成器、LCG、随机种子)
├── update.ts         # 版本更新与 APK 存在性检测服务
├── websocket.ts      # WebSocket 对决双人长连接管理与事件轮询处理
└── index.ts          # 主入口文件：配置路由器与路由处理分发
```

### 1. [types.ts](file:///e:/file/sheeps/server/src/types.ts) [NEW]
存放公用类型定义：
- `Env` 接口。
- 排行榜、关卡节点、版本更新、商店列表等相关的数据对象接口。

### 2. [crypto.ts](file:///e:/file/sheeps/server/src/crypto.ts) [NEW]
提取加解密逻辑：
- `generateJWT(payload)` 和 `verifyJWT(token)`
- `sha256(message)`

### 3. [helpers.ts](file:///e:/file/sheeps/server/src/helpers.ts) [NEW]
提取路由公共拦截器：
- `getCorsHeaders()`
- `getAuthenticatedUser(request, env)`
- `getLangSuffix(request)`

### 4. [level.ts](file:///e:/file/sheeps/server/src/level.ts) [NEW]
关卡生成算法单独解耦，独立维护，便于后续优化发牌难度与卡牌碰撞判断：
- `generateSolvableLevel(levelId, seed)`
- 关卡碰撞阻挡计算函数及辅助方法。

### 5. [update.ts](file:///e:/file/sheeps/server/src/update.ts) [NEW]
GitHub/DB 版本升级逻辑提取，保证 Apk 检测缓存全局有效。

### 6. [websocket.ts](file:///e:/file/sheeps/server/src/websocket.ts) [NEW]
WebSocket 建立长连接后的客户端连接管理、心跳、数据库轮询同步、攻击力及作弊计算校验。

### 7. [index.ts](file:///e:/file/sheeps/server/src/index.ts) [MODIFY]
清除所有杂项逻辑，作为主 Router 路由中转分发器，行数可压缩至 200 行以内，大大提高可读性。

---

## 影响文件清单
- **[NEW] [types.ts](file:///e:/file/sheeps/server/src/types.ts)**: 新建全局类型文件。
- **[NEW] [crypto.ts](file:///e:/file/sheeps/server/src/crypto.ts)**: 新建密码学工具模块。
- **[NEW] [helpers.ts](file:///e:/file/sheeps/server/src/helpers.ts)**: 新建辅助工具模块。
- **[NEW] [level.ts](file:///e:/file/sheeps/server/src/level.ts)**: 新建关卡生成算法文件。
- **[NEW] [update.ts](file:///e:/file/sheeps/server/src/update.ts)**: 新建版本检测服务文件。
- **[NEW] [websocket.ts](file:///e:/file/sheeps/server/src/websocket.ts)**: 新建 WebSocket 对决处理模块。
- **[MODIFY] [index.ts](file:///e:/file/sheeps/server/src/index.ts)**: 重构成极简路由入口。

---

## 验证计划
1. **自动构建部署验证**：
   - 运行 `npx wrangler deploy`，验证 TypeScript 模块导入编译是否成功，打包后能否成功发布至 Cloudflare。
2. **接口正确性验证**：
   - 使用客户端进行登录、签到、获取关卡与排行榜数据，确保在模块解耦后，接口的功能与请求时延优化不受任何影响。
