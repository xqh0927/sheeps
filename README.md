# 秘境消消乐

本系统是一个采用“中式民俗/秘境传说”视觉题材的层叠式三消益智游戏（类似于“羊了个羊”核心玩法）。项目整体采用前后端分离的“端云协同”架构开发：Android 客户端基于 Jetpack Compose + 现代 Android 架构设计，后端服务托管于 Cloudflare Workers 无服务器计算平台，结合 D1 分布式 SQLite 数据库进行云端存储。

---

## 🌟 项目亮点与核心功能

1. **端云协同的关关生成机制**：
   - 关卡布局数据基于特定种子在后端（Cloudflare Workers）进行确定性且必定有解（Solvable）的算法生成，包含 X/Y 轴半格对齐和多层 Z 轴层叠。
   - 网络异常时，Android 客户端会自动无缝降级为**本地离线模式**（Local-First），通过相同的 LCG 随机数生成算法在本地生成关卡，保证极致的游玩连贯性。

2. **丰富的关卡机制**：
   - **普通牌**：标准的可点击消除的图案牌。
   - **封印牌 (Sealed)**：表面贴有金黄色“封印符纸”的卡牌，其层叠封印符的透明度为 0.65，使玩家能够看清底部的图案，只有在周边卡牌被消除、触发解封时方可点击。
   - **神秘牌 (Blind)**：背面显示为“神秘问号”状态的暗牌，只有被揭开（最上层无遮挡）时才能显示真容。

3. **趣味道具与背包系统**：
   - 支持移出置物架（Move Out）、撤销（Undo）、洗牌（Shuffle）、提示（Hint）、炸弹（Bomb）、小丑牌（Joker）、双倍积分（Double Points）等多种法宝道具。
   - 道具可在积分商城中进行购买与兑换，数据支持云端同步和 Room 本地离线脏数据同步机制。

4. **全球实时排行榜**：
   - 关卡结算后，客户端通过带有加盐校验签名的防作弊算法，安全地将通关用时和得分上传至云端数据库，生成实时的日榜与总榜。

5. **社交与日常功能**：
   - 支持手机号验证码注册/登录、用户中心、每日签到（连签奖励）、每日任务、公告系统。

6. **多主题皮肤自绘系统**：
   - 包含经典国风、水墨江山、赛博霓虹三种独特界面主题。根据首选项自动切换，卡牌底色及 12 种游戏图案全部在 Compose Canvas 中通过图形学计算重绘（水墨黑灰写意色、霓虹特制电子荧光色）。

7. **法宝高清 Canvas 矢量自绘动画**：
   - 局内/备战 8 种道具卡（阴阳乾坤鱼、缩地祥云、天命星盘、还魂金丹、太极天眼镜、震天炸弹、炫彩八卦罗盘、双叠铜钱）及 3 种皮肤卡片全部废弃静态 Placeholder，改为纯 Canvas 像素级矢量自绘，搭载自转、漂浮、呼吸闪烁动效。

8. **智能发布流水线与一键对齐系统**：
   - 包含一键发布脚本 `release.js`，自动实现版本升级、向 D1 数据库写入记录并向 main 分支提交推送；GitHub Actions 自动解析 versionName 并与发布 Tag 对齐，彻底规避版本冲突死循环。

9. **多人实时对决与法术诅咒系统**：
   - **连击对决伤害**：支持多人 WebSocket 实时竞技，依据缓动公式 $Attack = Base \times (1 + \ln(Combo))$ 将消除连击转换为对手伤害，防范高频刷连击造成瞬杀，保证对战平衡。
   - **三大法术诅咒**：消除卡牌累积能量，可向对手释放实时法术诅咒。包含“迷雾咒”（遮挡法阵，探照清除）、“锁槽咒”（槽降为 6 格，满槽直接判负）和“封印咒”（给对方所有当前暴露牌施加封印）。
   - **重连退避与优雅挂起**：配备 15 秒断线挂起等待与指数退避重连机制，自动判定胜负，保障弱网体验。

10. **全盘汉化与地毯式详细中文注释补全**：
    - 项目中的 116 个源文件（包括后端核心 API 处理层 handlers、LCG 确定性求解生成算法，以及客户端公共库 `:core`、三消动画与渲染引擎 `:feature_game`、外围 UI 交互模块等）均已进行了地毯式的详细中文注释补全与重构润色，彻底消除多语言注释混杂问题，可读性与二开友好度极大提高。

---

## 🛠️ 核心框架技术与实现方式

### 📱 Android 客户端 (Android Client)

* **声明式 UI 渲染 (Jetpack Compose)**：
  - 全面摒弃传统 XML 布局，采用 Compose 声明式渲染整个游戏画境。局内卡牌叠放通过 `Box` 容器中动态测算每个卡牌的 `Modifier.offset` 来决定。结合 `graphicsLayer` 轻松实现压感弹性微缩放动效。
* **依赖注入 (Hilt / Dagger)**：
  - 采用 Hilt 构建全局依赖注入拓扑图。在 `:core` 模块中通过 `@InstallIn(SingletonComponent::class)` 注入全局单例（如 Room 数据库实例、网络 Retrofit 服务等），在 `:feature_*` 各业务模块中通过 `@ViewModelInject` 或 `@AndroidEntryPoint` 快捷获取。
* **数据流管理与异步架构 (Coroutines & Flow)**：
  - 基于 Kotlin 协程进行非阻塞式异步运算。ViewModel 与 View 之间通过 `StateFlow` 双向绑定状态，通过 `SharedFlow` 订阅一次性侧效应（如播放音效、振动反馈、Toaster 提示等），避免多线程竞争和内存泄漏。
* **离线本地优先存储 (Room ORM)**：
  - 本地采用 SQLite (Room) 缓存数据。为了实现 Local-First 的顺畅体验，用户购买道具、刷新进度时先写 Room 本地库，并将数据标记为“脏数据” (`isDirty = true`)，再通过后台协程同步器 `SyncRepository` 在网络恢复时自动排队向云端同步。
* **解耦多模块路由 (TheRouter)**：
  - 选用阿里开源的现代 Compose 路由框架 `TheRouter`，各个模块之间通过注册 URI（如 `/game/play`，`/menu/main`）实现页面间完全解耦跳转。
* **网络请求与日志拦截 (Retrofit 2 & OkHttp 3 & LogUtils)**：
   - 采用行业标准的 `Retrofit 2` 作为网络访问接口层，结合 `Kotlinx.serialization` 进行类型安全的 JSON 数据解析。
   - 底层使用 `OkHttp 3` 客户端进行连接管理、超时配置。同时配置了四大拦截器：语言国际化拦截器、Bearer Token 身份认证拦截器、带锁防并发的双 Token 静默刷新拦截器、以及弱网自动指数退避重试拦截器。
   - **网络与系统日志分析**：集成流行日志库 `pengwei1024/LogUtils` (`com.apkfuns.logutils:library`)，配合 `HttpLoggingInterceptor` 拦截网络请求，整包聚聚合以无 Tag（利用其自动调用类名堆栈追踪定位机制）的方式输出结构化调试日志，极大地方便了端云联合调试。
* **Canvas 纯矢量高清自绘与微交互动画**：
  - 局内卡牌渲染与 8 种道具卡全部基于 Compose Canvas 矢量图形学绘制，彻底摆脱传统位图占用，并结合 `InfiniteTransition` 实现自转、漂浮、呼吸闪烁动效。
* **自定义 Q弹 Spring 物理动效 (Compose Transition)**：
  - 自定义封装 PrepareGameDialog 弹窗，利用 `animateFloatAsState` 实现具有物理惯性的弹性入场（Q弹）与淡入淡出动画，显著提升操作质感。


### ☁️ 后端服务 (Backend Service)

* **Serverless 边缘轻量计算 (Cloudflare Workers & TypeScript)**：
  - 采用无服务器计算架构，无冷启动延迟。业务路由基于 TypeScript 构建，在全球数十个边缘节点就近响应客户端请求。
* **边缘分布式 SQLite (Cloudflare D1)**：
  - 后端直接绑定 D1 分布式 SQLite 数据库，实现轻量高速的 SQL 查询。用户配置、关卡模板、实时排行榜、签到流水账等数据均在 D1 中原子落盘。
* **KV 边缘缓存加速 (Cloudflare Workers KV)**：
  - 公告、排行榜、关卡布局、商城道具、系统配置等读高频数据全部通过 KV 进行边缘持久化缓存（300s-24h TTL），大幅降低 D1 读取负载和 API 响应延迟。
* **R2 对象存储 + 自定义域名 CDN (Cloudflare R2)**：
  - APK 安装包托管于 R2（免费 10GB + 零出口流量费），经自定义域名 `apk.xqh.cc.cd` 全球 CDN 边缘分发，告别 GitHub Releases 国内下载缓慢问题。
* **JWT 密钥 Cache + D1 查询合并优化**：
  - WebCrypto `importKey` 从每次调用改为 Worker 实例级单例缓存，省 30-40% CPU；
  - 登录接口 3 次独立 D1 batch 合并为 1 次读 + 最多 1 次写。


---

## ⚙️ 全局统一配置规范

为了避免多处散落、硬编码相同全局信息（如服务器接口根地址 `BASE_URL`），项目在 `:core` 模块根包名 `com.example.sheeps.core` 下建立并封装了 **`AppConfig`** 对象：
- **`AppConfig`** ([AppConfig.kt](/file/app/core/src/main/java/com/example/sheeps/core/AppConfig.kt))：作为应用唯一的全局配置入口，所有的环境常量、协议配置以及未来的功能开关都会统一收拢到该对象中。
- 移除了原有的 `ApiService.Companion.create` 冗余代码，使整个网络的配置点实现一处声明、全局消费。

---

## 🧰 封装的常用核心工具类说明

在 `:core` 基础库中的 `com.example.sheeps.core.utils` 包下，项目针对通用系统级功能进行了二次封装，提供了极简且防错的接口：

1. **`NetworkMonitor` (实时网络状态监听类)**：
   - **实现机制**：内部利用 Android `ConnectivityManager` 的 `registerNetworkCallback` 机制主动监听系统网络变化。
   - **功能作用**：实时维护一个公开的 `StateFlow<NetworkStatus>`。当网络发生“可用 (onAvailable)”和“丢失 (onLost)”状态转换时自动更新。对外提供极其便利的同步判断接口 `isOnline(): Boolean`，业务 ViewModel 在发起关卡加载前会据此自动判断应从云端 API 获取关卡还是降级到本地 LCG 生成算法。

2. **`AuthEventBus` (全局认证事件总线)**：
   - **实现机制**：基于 `MutableSharedFlow` 封装的单例事件总线，采用 `extraBufferCapacity = 1` 确保即使在协程挂起时事件也不会丢失。
   - **功能作用**：主要用于处理 App 生命周期内的全局鉴权敏感事件（如 `AuthEvent.Logout`）。当网络层拦截器检测到 HTTP 401 凭证过期或服务器提示令牌失效时，会通过 `AuthEventBus.post(AuthEvent.Logout)` 发送事件，主界面监听到后会自动清除 Preference 凭证并一键切回登录页。

3. **`GsonUtil` (JSON 数据高阶序列化工具)**：
   - **实现机制**：基于 Google `Gson` 库进行二次封装，内部维护全局共享的 Gson 实例。
   - **功能作用**：提供统一的 `toJson(src)`、`fromJson(json, classOfT)` 和支持泛型反射 Type 的 `fromJson(json, typeOfT)`。主要用于背包道具复杂 JSON 字符串在 Room 数据库文本字段与 Kotlin 数据类之间的便捷映射。

---

## 📂 项目目录结构

```text
├── app/                        # Android 客户端代码
│   ├── app/                    # 壳 Module，主程序入口 (SplashActivity, 依赖初始化)
│   ├── core/                   # 核心基础 Module (包含网络、Room本地数据库、数据模型、存储等)
│   ├── feature_game/           # 游戏局内 Module (GameActivity, ViewModel, 三消核心渲染及手势逻辑)
│   ├── feature_leaderboard/    # 排行榜独立 Module (局内/局外排行榜展示)
│   ├── feature_menu/           # 主菜单与商店 Module (签到、公告、任务、道具兑换)
│   ├── build.gradle.kts        # 根构建文件
│   └── gradlew.bat             # Gradle 包装脚本
│
├── server/                     # 后端服务代码 (Cloudflare Worker)
│   ├── src/
│   │   ├── index.ts            # Workers 路由入口
│   │   ├── handlers/           # 业务路由 (auth/game/shop/system/user)
│   │   ├── crypto.ts           # JWT 签发校验 + 防作弊签名
│   │   ├── level.ts            # LCG 确定性关卡生成算法
│   │   ├── difficulty.ts       # 100 级难度系数系统
│   │   └── helpers.ts          # 通用辅助 (鉴权/CORS/KV配置缓存)
│   ├── test/                   # 后端单元测试 (level/difficulty/update)
│   ├── wrangler.jsonc          # Cloudflare Wrangler 配置 (Worker + D1 + KV + R2)
│   ├── schema.sql              # D1 数据库初始化建表脚本
│   └── package.json
│
└── docs/                       # 项目配套文档
```

---

## 🚀 快速启动指南

### 1. 后端服务部署 (Cloudflare Worker)
在 `server` 目录下配置并发布边缘端代码：

1. 安装依赖：
   ```bash
   cd server
   npm install
   ```
2. 登录 Cloudflare 账号：
   ```bash
   npx wrangler login
   ```
3. 初始化 D1 + KV + R2（需提前在 Cloudflare Dashboard 创建）：
   - D1 数据库名称：`my-app-db`
   - KV 命名空间：`SHEEPS_CACHE`
   - R2 存储桶：`sheeps-apk`
4. 将 `wrangler.jsonc` 中的 `database_id`、`kv_namespaces[].id` 替换为创建生成的 UUID。
5. 初始化数据库表结构：
   ```bash
   npx wrangler d1 execute my-app-db --remote --file=./schema.sql
   ```
6. 部署到 Cloudflare 全球网络：
   ```bash
   npx wrangler deploy
   ```

### 2. 一键版本发布脚本 (release.js)
项目根目录下的 `node release.js` 实现全自动化发版流程：

1. **交互式版本管理**：自动解析 `build.gradle.kts` 当前版本并建议下个版本号。
2. **Git 前置安全校验**：自动检查当前是否在 `main` 分支，并在提交前 `pull --rebase` 同步远程代码，防止冲突和误推。
3. **多语言自动翻译**：通过 Google Translate API 将更新日志自动翻译为英语/日语/韩语。
4. **D1 数据库同步**：将版本信息写入 `app_version` 表，同时插入多语言公告至 `notice` 表。
5. **KV 缓存即时更新**：追加各语言公告缓存，确保客户端即刻可见最新公告。
6. **全量代码提交**：`git add -A` + commit + push 到 main 分支，触发 GitHub Actions 编译打包。
7. **R2 对象存储上传**：GitHub Actions 构建 APK 成功后自动上传到 Cloudflare R2（通过 S3 兼容 API），经 `apk.xqh.cc.cd` 自定义域名 CDN 分发。

### 3. Android 客户端编译与运行
在 `app` 目录下：

1. 确认配置：
   - 检查 `app/core/src/main/java/com/example/sheeps/core/AppConfig.kt` 中的 `BASE_URL` 指向您所部署的 Cloudflare Workers 域名（默认为：`https://xqh.cc.cd/`）。
2. 构建 Debug 安装包：
   ```bash
   ./gradlew :app:assembleDebug
   ```
3. 通过 ADB 安装到手机：
   ```bash
   adb install app/app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📐 核心算法与设计决策

### 1. 关卡三维金字塔布局与 10% 面积碰撞重叠判定
- **金字塔堆叠**：通过网格尺寸逐层递减 1 ($size = baseSize - z$) 且偏移量逐层累加 0.5 ($offset = z \times 0.5f$) 的循环逻辑，实现层次分明、上疏下密的三维立体堆叠。
- **10% 重叠判定**：单卡尺寸统一为 `48dp`（面积 $2304\text{ dp}^2$）。两张卡牌重叠的计算公式为：
  $$ox = \max(0, 48f - dx \times 46f)$$
  $$oy = \max(0, 48f - dy \times 46f)$$
  $$area = ox \times oy$$
  当且仅当 $area > 230.4\text{ dp}^2$（即重叠面积 > 10%）且高度更高时，建立遮挡/锁定关系。本机制在客户端（运行时判定与本地求解器）与服务端（Cloudflare Workers 求解器）双端完全对齐，保证关卡 **100% 可解通**。

### 2. 双轴棋盘自适应居中对齐
- 采用 Compose 的 `LocalConfiguration.current.screenWidthDp.dp` 动态读取物理屏幕宽度，宽度公式为 `boardWidth = screenWidth - 32.dp`，确保两边各留出 16dp 对称空白。
- 整个棋盘高度根据渲染的卡牌高度集合自适应（上下各留白 16dp），并利用父级 Box 的 `contentAlignment = Alignment.Center`，使整个异形卡牌堆叠在任何尺寸手机/折叠屏上都完美水平和垂直双居中渲染，彻底杜绝了偏边、越界或裁剪问题。

### 3. 卡牌飞行动画 GPU 硬件加速设计
- 传统 Compose 位移动画会在每一帧中高频读取 progress 状态触发全量 Compose Recomposition (重组) 和 Relayout (重测量)，极易造成 CPU 瞬间阻塞导致卡顿。
- 本项目重构了位移计算，将 progress 状态读取延迟至 Draw 绘图阶段，使用 **`Modifier.graphicsLayer { ... }`** 直接对卡牌应用 `translationX`/`translationY` 及 `scale`。
- **效果**：所有位移及大小动画计算完全下沉至 **GPU 硬件级加速** 完成，在低端 Android 设备上依旧能保持 60/120 帧丝滑消除体验。

### 4. 太极万能牌消除平衡算法
- 万能牌在卡槽中必须至少存在 2 张同花色卡牌时才能激活（否则报错并拒绝扣减道具）。
- 成功激活时，会自动在卡槽外（优先从移出置物架，其次从棋盘在场卡牌中）搜索并一同销毁第 3 张相同花色卡牌，保证局内该花色卡牌总数继续保持 3 的倍数，在体验爽快道具的同时，为**完全消除并通关**提供了数学层面的绝对保障。

### 5. 关卡类型全局概率与 LCG 同源匹配机制
- 关卡在加载时会通过确定性 LCG 算法 `lcg(seed + 500)` 进行全局特质判定：
  - **40% 封印关卡 (Sealed Level)**：包含金色锁头阻挡，需玩家自行点击破封。
  - **20% 盲盒关卡 (Blind Level)**：包含迷雾暗牌问号牌，且最表层 2~3 层卡牌禁止设为盲盒牌以确保前 3-5 步可解。
  - **40% 正常关卡 (Normal Level)**：经典常规美食三消卡牌。
- 前置准备弹窗 `PrepareGameDialog` 同源运行该 LCG 预测，并在进入盲盒关卡时安全精准地为玩家渲染橙红色盲盒警示 Banner。

### 6. 盲盒牌解密解锁与锁定抖动物理反馈
- **盲盒牌解锁**：盲盒牌在棋盘上不因“解除上层卡牌遮挡”而翻开。它的唯一翻开解密方式是**玩家在下方槽位完成一组 3 连匹配消除（Match/Merge）**。每消除一组牌，系统自动揭晓并翻开棋盘上的某一张盲盒牌，大大增强了战术博弈性。
- **只抖直接遮挡物**：点击被锁定的卡牌时，系统运用 Z 轴极值算法计算出直接覆盖在其上方的临近卡牌，并使其轻微平滑抖动一次（`repeat(1)`）红光反馈，其余间接压住的上层卡牌保持完全静止，物理反馈克制、直观。

### 7. 列表平滑定位与冷启动防死锁
- **跨度平滑滚动**：扩展编写了 `LazyListState.animateScrollToItemSmoothly`。当定位跨度大于 4 时，先无缝 Snap 跳转到目标项临近 3 个元素内，再以平滑过渡动画滚动至最终位置，极大减少了 LazyColumn 渲染中间大量无用 Item 时对 CPU 造成的严重卡顿负担。
- **异步加载防锁死**：私有文件级变量 `isColdStartAutoScrolled` 结合 LaunchedEffect 超时轮询。若首帧读到 unlockedLevel 占位符 1，不锁死自动滚动标志，而是等待本地/云端异步数据加载完毕后，再平滑定位到玩家真实的解锁关卡位置，并锁定滚动标志。
