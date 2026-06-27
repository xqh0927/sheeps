# 国风消除游戏扩展与 Compose 全面重构实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 彻底废除 XML 布局，使用 100% 纯 Jetpack Compose 实现启动页、主页多 Tab 切换（游戏列表、商城、个人中心）和排行榜，同时在三端（Android、Server、Database）闭环集成手机号+验证码登录（JWT 验证）、关卡解锁、积分商城、签到与每日任务，并对客户端的所有 Retrofit 请求实现详尽日志和异常自动拦截。

**架构：**
1.  **D1 数据库**：补充 12 张新表以支撑完整的状态流水。
2.  **Worker API**：通过 Web Crypto API 实现原生 HMAC-SHA256 的 JWT 签发和认证。
3.  **Android Core**：通过 MMKV 保存 JWT，自定义 OkHttp Interceptor 记录详细网络日志，并统一封装 Exception 映射为 ViewEffect。
4.  **MVI & Compose**：重构 Activity，主页面 `MenuActivity` 作为底部导航（Game/Shop/Me）宿主，全面废除 ViewBinding。

**技术栈：** Kotlin, Jetpack Compose, Coroutines, StateFlow, Hilt, MMKV, Retrofit, OkHttp 3, Cloudflare Workers, TypeScript, Cloudflare D1.

---

## 实施步骤与任务分解

### 任务 1：升级 D1 数据库结构并导入初始配置

**文件：**
*   修改：`server/schema.sql`
*   修改：`server/wrangler.toml`

- [ ] **步骤 1：修改数据库建表脚本**
  在 `server/schema.sql` 中新增/修改以下表定义，确保覆盖 `users`、`login_token`、`level_unlock`、`user_items`、`shop_items`、`exchange_record`、`point_record`、`sign_record`、`task`、`user_task`、`notice`、`config`。
  
  ```sql
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
  DROP TABLE IF EXISTS users;

  CREATE TABLE users (
      id TEXT PRIMARY KEY,
      phone TEXT UNIQUE,
      username TEXT NOT NULL,
      avatar TEXT,
      points INTEGER DEFAULT 0,
      created_at INTEGER NOT NULL
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

  CREATE TABLE leaderboard (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id TEXT NOT NULL,
      level_id INTEGER NOT NULL,
      score INTEGER NOT NULL,
      clear_time_ms INTEGER NOT NULL,
      achieved_at INTEGER NOT NULL,
      FOREIGN KEY(user_id) REFERENCES users(id)
  );
  
  -- 导入初始任务、公告、商城商品和系统配置配置
  INSERT INTO task (id, name, description, target_count, points_reward) VALUES 
  ('PLAY_3_GAMES', '小试牛刀', '游玩3局游戏', 3, 20),
  ('PLAY_5_GAMES', '大显身手', '游玩5局游戏', 5, 40),
  ('SIGN_IN_ONCE', '每日晨曦', '完成一次签到', 1, 10);

  INSERT INTO notice (title, content, type, created_at) VALUES 
  ('国风消消乐2.0盛大开启', '喜迎全新水墨修真版本，多重福利送不停！', 'ACTIVITY', 1720000000000),
  ('版本优化公告', '优化了层叠卡牌飞入和重叠状态计算性能', 'UPDATE', 1720000000100);

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
  ('sign_rewards', '20,20,30,30,40,50,100');
  ```

- [ ] **步骤 2：在本地初始化 D1 数据库**
  运行：`npx wrangler d1 execute DB --local --file=schema.sql`
  预期：数据库表及基础数据成功导入，命令行输出 "Success"。

---

### 任务 2：实现 Cloudflare Workers 后端核心功能 (JWT 认证、登录、同步、商城、签到、任务及配置)

**文件：**
*   修改：`server/src/index.ts`

- [ ] **步骤 1：实现 JWT 生成与解析**
  在 `server/src/index.ts` 头部实现轻量级 JWT 算法，利用 Web Crypto API (HMAC-SHA256)。
  
  ```typescript
  const JWT_SECRET = 'antigravity_secret_key';

  async function generateJWT(payload: any): Promise<string> {
    const header = { alg: 'HS256', typ: 'JWT' };
    const encodedHeader = btoa(JSON.stringify(header)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    const encodedPayload = btoa(unescape(encodeURIComponent(JSON.stringify(payload)))).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    
    const key = await crypto.subtle.importKey(
      'raw',
      new TextEncoder().encode(JWT_SECRET),
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    );
    const signatureBuffer = await crypto.subtle.sign(
      'HMAC',
      key,
      new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`)
    );
    const signature = btoa(String.fromCharCode(...new Uint8Array(signatureBuffer)))
      .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');

    return `${encodedHeader}.${encodedPayload}.${signature}`;
  }

  async function verifyJWT(token: string): Promise<any | null> {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const [encodedHeader, encodedPayload, signature] = parts;
    
    const key = await crypto.subtle.importKey(
      'raw',
      new TextEncoder().encode(JWT_SECRET),
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['verify']
    );
    const expectedSigBuffer = await crypto.subtle.sign(
      'HMAC',
      key,
      new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`)
    );
    const expectedSig = btoa(String.fromCharCode(...new Uint8Array(expectedSigBuffer)))
      .replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
      
    if (signature !== expectedSig) return null;
    
    try {
      const decodedPayload = decodeURIComponent(escape(atob(encodedPayload.replace(/-/g, '+').replace(/_/g, '/'))));
      return JSON.parse(decodedPayload);
    } catch {
      return null;
    }
  }
  ```

- [ ] **步骤 2：实现 Auth 验证拦截辅助器**
  定义 `getAuthenticatedUser(request, env)` 函数，根据 Header 中的 `Authorization` 解析出对应的用户。

- [ ] **步骤 3：编写完整的 API 路由逻辑**
  修改 `server/src/index.ts` 的 `fetch` 入口，添加以下路由支持：
  1.  `POST /api/auth/send-code`：存储验证码，并直接返回验证码。
  2.  `POST /api/auth/login`：校验验证码。若用户不存在则自动注册。生成并返回 JWT，以及全部个人数据和背包。
  3.  `POST /api/user/sync` (Auth)：上传离线数据，云端更新并同步。
  4.  `POST /api/level/unlock` (Auth)：扣除相应配置积分，并在 `level_unlock` 表中插入记录。
  5.  `POST /api/score/submit` (Auth 可选)：修改原本的提交逻辑。如果是登录用户，通关后解锁下一关，增加任务进度，并上传排行榜。
  6.  `GET /api/shop/items`：返回商品。
  7.  `POST /api/shop/exchange` (Auth)：验证扣减积分和库存，发放道具，记录 `exchange_record` 流水。
  8.  `POST /api/sign/today` (Auth)：校验当天是否签到，获取签到天数和对应的奖励配置积分，记录 `sign_record` 与流水。
  9.  `GET /api/task/daily` (Auth)：统计今日任务进度并返回。
  10. `POST /api/task/claim` (Auth)：标记任务已领奖并增加积分。
  11. `GET /api/notice/list`：返回公告列表。
  12. 管理员及全局配置 `GET/POST /api/admin/config`。

- [ ] **步骤 4：测试 API 可用性**
  运行本地 Worker 并模拟接口请求验证正确性。

---

### 任务 3：构建 Android 客户端数据与网络拦截器

**文件：**
*   修改：`app/core/src/main/java/com/example/sheeps/data/model/GameModels.kt`
*   修改：`app/core/src/main/java/com/example/sheeps/data/network/ApiService.kt`
*   修改：`app/core/src/main/java/com/example/sheeps/core/di/NetworkModule.kt`
*   修改：`app/core/src/main/java/com/example/sheeps/core/preference/UserPreferences.kt`

- [ ] **步骤 1：扩展 `GameModels.kt` 实体**
  按照设计，增加 `UserInfo`、`UserItem`、`ShopItem`、`ExchangeRecord`、`PointRecord`、`DailyTask`、`Notice` 模型，以及登录相关的 `SendCodeRequest`、`SendCodeResponse`、`LoginRequest`、`LoginResponse`、`UnlockLevelRequest`、`ExchangeRequest`、`ExchangeResponse`、`SignResponse`、`TaskClaimRequest`、`SyncRequest` 模型。

- [ ] **步骤 2：实现 Retrofit Logging okhttp.Interceptor 打印详细日志**
  在 `NetworkModule.kt` 旁或内部编写 `HttpLoggingInterceptor`，对每一个 API 打印格式化日志（Url、Method、Headers、Body、Response Code、Response Message、Response Body 以及 Time Elapsed）。对于请求抛出的异常，使用 Hilt 将其注入。
  
- [ ] **步骤 3：在 `ApiService.kt` 声明新增接口**
  声明全部 RESTful 请求，并支持携带 `Authorization` 头部。

- [ ] **步骤 4：在 `UserPreferences.kt` 增加 Token 存储**
  使用 MMKV 存储 `jwt_token` 和当前登录的 `user_phone`，并封装 `logout()` 函数清空状态。

---

### 任务 4：重构 `SplashActivity`（完全废弃 XML）

**文件：**
*   修改：`app/feature_splash/src/main/java/com/example/sheeps/splash/SplashActivity.kt`
*   删除：`app/feature_splash/src/main/res/layout/activity_splash.xml`

- [ ] **步骤 1：删除 `activity_splash.xml`**
  运行本地命令或手动删除该 XML 布局。

- [ ] **步骤 2：重写 `SplashActivity` 继承 `BaseActivity`**
  使用 `setContent {}` 渲染全新的国风 Splash Screen。
  展示如下文字：
  *   “抵制不良游戏，拒绝盗版游戏。注意自我保护，谨防受骗上当。适度游戏益脑，沉迷游戏伤身。合理安排时间，享受健康生活。”
  *   CADPA 适龄提示绿标（8+）。
  *   隐私政策弹窗改用 Compose `AlertDialog` 重写。

---

### 任务 5：重构 `LeaderboardActivity`（完全废弃 XML）

**文件：**
*   修改：`app/feature_leaderboard/src/main/java/com/example/sheeps/leaderboard/LeaderboardActivity.kt`
*   删除：`app/feature_leaderboard/src/main/res/layout/activity_leaderboard.xml`
*   删除：`app/feature_leaderboard/src/main/res/layout/item_ranking.xml`
*   删除：`app/feature_leaderboard/ui/adapter/LeaderboardAdapter.kt`

- [ ] **步骤 1：删除布局 XML 及适配器类**
  删除 RecyclerView 的 XML、Adapter 和 Activity 绑定的 Layout 文件。

- [ ] **步骤 2：用 Compose 重构 `LeaderboardActivity` 继承 `BaseActivity`**
  *   使用 `setContent` 重写整个页面。
  *   在 UI 中设计三个子 Tab 选项：“今日排行”、“本周排行”、“历史排行”。
  *   使用 LazyColumn 网格/列表渲染排行记录，为前三名设计带有国风底纹和金/银/铜标的样式，且支持分页加载。

---

### 任务 6：实现主页面 `MenuActivity` 的多 Tab 容器（Game/Shop/Me）与核心 UI

**文件：**
*   修改：`app/feature_menu/src/main/java/com/example/sheeps/menu/MenuActivity.kt`

- [ ] **步骤 1：设计主页框架（`Scaffold` + `NavigationBar`）**
  在 `MenuActivity` 内部实现主界面导航控制，定义 Tab 枚举：`GAME`、`SHOP`、`ME`。
  根据选中状态分别显示：
  1.  `GameHomeTab`
  2.  `ShopTab`
  3.  `PersonalTab`

- [ ] **步骤 2：实现 `GameHomeTab` 游戏关卡页**
  *   顶部渲染简易玩家卡片，未登录时显示“游客模式”及“点击登录”。
  *   横向跑马灯/卡片展示系统公告，点击可看大图/详情。
  *   关卡列表展示。当点击第 4 关及后续关卡，判定未登录则弹出登录框。
  *   点击可玩关卡，弹出**关卡备战弹窗**。用户可以选择携带的道具，点击“正式游玩”带入关卡。

- [ ] **步骤 3：实现 `ShopTab` 积分商城页**
  *   未登录时，显示全屏蒙层锁定并提示“请先登录后再进行神兵法宝兑换”。
  *   登录后：网格展示 8 种道具的价格、库存，点击可以扣除积分兑换为背包道具。

- [ ] **步骤 4：实现 `PersonalTab` 个人中心页**
  *   展示 UID、手机号、积分余额、连续签到天数。
  *   **签到日历面板**：一键签到，根据连续签到天数从后端获取奖励并弹窗动画。
  *   **道具栏**：展示当前背包全部 8 种道具的库存值。
  *   支持列表点击弹窗查看“积分流水”与“兑换历史”。
  *   设置、关于、退出登录等按钮。

---

### 任务 7：修改 `GameViewModel` 适配备战道具携带及新增 4 种功能道具

**文件：**
*   修改：`app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/GameViewModel.kt`
*   修改：`app/feature_game/src/main/java/com/example/sheeps/game/state/GameMviContract.kt`
*   修改：`app/feature_game/src/main/java/com/example/sheeps/game/ui/screens/GameScreen.kt`

- [ ] **步骤 1：在 `GameMviContract.kt` 中拓展道具意图**
  添加新增的 4 种道具使用 Intent：`UseHint`、`UseBomb`、`UseJoker`、`UseDoublePoints`，以及备战数据传入参数。

- [ ] **步骤 2：修改 `GameViewModel.kt` 的关卡载入与道具扣减**
  在 `LoadLevel` 时，接收携带的道具数量并重置当局的道具可用上限。
  实现 4 种新道具的核心算法逻辑：
  *   **提示 (Hint)**：遍历棋盘上所有非遮挡的卡牌，计算出相同 `type` 的 3 张牌，高亮展示。
  *   **炸弹 (Bomb)**：直接从卡槽中移除最后两张放入的卡牌（直接销毁，不加积分，不清零）。
  *   **万能牌 (Joker)**：点击使用后，万能牌直接进入卡槽，其 `type` 自动转为当前卡槽内数量最多或倒数一两张相同的卡牌 `type`，达到一键凑对消除效果。
  *   **双倍积分卡 (Double Points)**：在通关结算前使用，当局得分乘以 2 上传。

- [ ] **步骤 3：修改 `GameScreen.kt` 的棋盘与底部道具栏 UI**
  在棋盘底部展示 8 个道具格（包括 Undo, Shuffle, MoveOut, Revive, Hint, Bomb, Joker, Double），并显示本局可用数量。数量为 0 时不可点击。

---

## 验证与发布

- [ ] **自动化测试校验**
  编写 `solvability` 与 `jwt_auth` 的测试覆盖。

- [ ] **运行三端并手动回归**
  *   用游客模式体验 1..3 关。
  *   输入手机号和获取验证码登录，成为正式用户。
  *   完成每日签到和商城道具兑换，验证背包数量和积分变动成功同步后端。
  *   带入道具进入游戏，并使用新道具通关，验证上传排行榜成绩正确。
