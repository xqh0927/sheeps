# 秘境消消乐 - 中式民俗传说主题三消手游 (Fold-消除类)

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
│   │   ├── index.ts            # Workers API 路由与业务逻辑实现 (包含注册、登录、关卡生成、排行榜、签到等)
│   ├── wrangler.toml           # Cloudflare Wrangler 配置文件 (配置路由、D1 绑定、环境变量)
│   ├── package.json
│   └── tsconfig.json
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
3. 创建 D1 数据库绑定：
   ```bash
   npx wrangler d1 create sheeps-db
   ```
4. 将 `wrangler.toml` 中的 `database_id` 替换为创建生成的 UUID。
5. 初始化数据库表结构：
   在 `server/` 根目录执行初始化 SQL：
   ```bash
   npx wrangler d1 execute sheeps-db --local --file=./schema.sql
   npx wrangler d1 execute sheeps-db --remote --file=./schema.sql
   ```
6. 部署到 Cloudflare 全球网络：
   ```bash
   npm run deploy
   ```

### 2. 一键版本发布与 D1/CI 自动化同步脚本 (release.js)
为了彻底规避客户端与云端版本不一致导致的“无限循环提示更新”死循环，项目在根目录下集成了高度自动化的发布系统：

1. **工作原理**：
   - 当需要打包发布新版本时，在根目录下运行 `node release.js` 交互式命令。
   - 脚本自动解析 `build.gradle.kts` 当前版本并自动建议下个版本。您只需输入“更新说明日志”。
   - 脚本自动修改本地版本配置，动态生成 SQL 并执行 `wrangler d1 execute` 插入到云端 D1 数据库中。
   - 随后，脚本自动完成 Git Commit 并 Push 提交到 main 分支。
2. **精细化 CI 触发过滤**：
   - GitHub Actions 的 build 打包任务配置了消息过滤：`if: contains(github.event.head_commit.message, 'chore(release):')`。
   - 任何日常手动 `git push`（不包含 `chore(release):` 的常规开发）都**不会触发**耗时的 App 构建；只有通过 `release.js` 发布的提交，才会精准触发 Actions 进行 Release 编译并以 `v${versionName}` 自动生成 Release Tag。
3. **服务端 APK 延迟发布与多版本容错机制**：
   - **HEAD 状态检测**：为避免 D1 数据库插入后、GitHub Actions 构建上传 APK 完成前，客户端因读取到高版本发起更新而导致下载 404 错误，Cloudflare Worker 端在检测到高版本时，会主动以 `HEAD` 轻量级报文请求对应版本 APK。只有当链接可达（200 OK）时才推送该版本更新。
   - **多版本平滑过滤**：如果最新版本还在打包，Worker 会向下查找已编译完成的最近可用高版本进行推送；同时设置了内存缓存（200成功缓存 24 小时，404构建中缓存 60 秒）以优化检测效率并避免速率限制。

### 3. Android 客户端编译与运行
在 `app` 目录下：

1. 确认配置：
   - 检查 `app/core/src/main/java/com/example/sheeps/data/network/ApiService.kt` 中的 `BASE_URL` 指向您所部署的 Cloudflare Workers 域名（默认为：`http://xqh.cc.cd/`）。
2. 构建 Debug 安装包：
   ```powershell
   ./gradlew.bat :app:assembleDebug
   ```
3. 通过 ADB 部署到实体手机：
   - 确保手机开启了 **USB调试** 并且连接正常（通过 `adb devices` 可查询到设备标识符，当前为 `40ece328`）。
   - 执行安装并拉起应用命令：
     ```powershell
     android run --device=40ece328 --apks=C:\Users\15613\Documents\file\app\app\build\intermediates\apk\debug\app-debug.apk
     ```

---

## 📐 核心算法与设计决策

### 1. 关卡三维布局与确定性有解生成
为了避免随机生成导致死局，关卡生成流程如下：
- 定义 3D 坐标系，计算卡牌的覆盖碰撞面积关系（若 `a.z > b.z && |a.x - b.x| < 1.0f && |a.y - b.y| < 1.0f`，则 `a` 遮挡 `b`）。
- 倒序模拟玩家反向消除卡牌的过程（从最上层完全暴露的卡牌开始放入图案，每次以 3 张为一组进行反向渲染），确保正向游玩时所有卡牌都可以按组完全消除。

### 2. 界面自适应完美居中对齐
使用 Compose 的 `BoxWithConstraints` 动态获取可用空间，并借助 `contentAlignment = Alignment.Center` 配合大小限制：
- 在布局前，遍历可见卡牌计算其 `minX`, `maxX`, `minY`, `maxY` 的逻辑坐标极值。
- 通过极值差距计算出卡牌组合的精确设计物理大小（`boardWidth` / `boardHeight`）。
- 给包裹卡牌的子 Box 赋予该固定尺寸，使得卡牌集合作为一个整体在灰色大框正中间渲染，解决不同屏幕比例下卡牌被裁剪、越界或偏置的问题。

### 3. 微动效与触觉灵敏度
在 `TileView.kt` 中设计了非阻塞式手势缩放弹性反馈：
- 观察 `collectIsPressedAsState()`，结合 `animateFloatAsState` 动画。
- 触碰未被封印与非遮挡的卡牌瞬间，卡牌触发缓动 spring 弹性瞬间缩放至 `90%`。
- 松手或滑出卡牌时自动弹回 `100%`，确保玩家即使在微调角度点击时也能得到立竿见影的灵敏触感。
