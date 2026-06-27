# 游戏组件化重构与现代框架集成实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标**：重构消除游戏，将 Android 端代码拆分成组件化架构，使用 **TheRouter** 进行组件通信与路由。引入 Blankj **AndroidUtilCode**、腾讯 **MMKV**、**Gson**、**Lottie**，使用 **XXPermissions** 处理权限，使用 **Toaster** 吐司，使用 **Logcat** 日志，使用 **Coil** 加载 Webp 格式图片消除牌，排行榜模块采用传统 `RecyclerView` 结合 **SmartRefreshLayout** 和 **BaseRecyclerViewAdapterHelper (BRVAH)** 适配器实现下拉刷新。图片资源采用 **Webp** 格式并引入 **ScreenMatch** 屏幕适配方案，同时在 `:core` 模块中抽取高复用性的 **BaseActivity** 和 **BaseMviViewModel** 基类以简化业务逻辑。

**架构**：
- **`:app`**：壳工程，继承 Application 并进行所有第三方库 (MMKV, Logcat, Toaster, TheRouter) 的初始化。配置 **ScreenMatch** 配置文件。
- **`:core`**：核心基础模块，提供通用网络客户端、MMKV 本地存储实现、GSON 解析工具、Webp 卡牌资源、基础适配 `dimens.xml`，以及可复用基类 `BaseActivity`（带 ViewBinding & 沉浸式）和 `BaseMviViewModel`。
- **`:feature_splash`**：启动页组件，继承自 `BaseActivity`，提供 XXPermissions 动态权限申请与工信部隐私合规审查界面。
- **`:feature_menu`**：菜单组件，继承自 `BaseActivity`，提供国风大厅与关卡解锁选择。
- **`:feature_game`**：核心消除玩法组件（Compose 实现，继承自 `BaseActivity`）。
- **`:feature_leaderboard`**：排行榜组件，继承自 `BaseActivity`（RecyclerView + SmartRefreshLayout + BRVAH 适配器实现）。

**技术栈**：Kotlin, Jetpack Compose, XML Views, TheRouter, XXPermissions, Toaster, Logcat, AndroidUtilCode, Coil, SmartRefreshLayout, BRVAH (v3), MMKV, GSON, Lottie, ScreenMatch.

---

## Proposed Changes

### 1. 全局配置与适配

#### [MODIFY] [settings.gradle.kts](file:///c:/Users/15613/Documents/file/app/settings.gradle.kts)
- 引入 JitPack 仓库以获取 getActivity 相关库和 AndroidUtilCode。
- 引入 `:core`、`:feature_splash`、`:feature_menu`、`:feature_game`、`:feature_leaderboard` 模块。

#### [MODIFY] [build.gradle.kts (Root)](file:///c:/Users/15613/Documents/file/app/build.gradle.kts)
- 引入 Kapt 插件与 TheRouter 插件。

#### [MODIFY] [libs.versions.toml](file:///c:/Users/15613/Documents/file/app/gradle/libs.versions.toml)
- 新增/更新所有第三方依赖的版本和库定义。

#### [NEW] [screenmatch.properties](file:///c:/Users/15613/Documents/file/app/screenmatch.properties)
- ScreenMatch 屏幕适配的配置文件，指定基准屏幕宽度为 360dp，并生成适配中国主流手机屏幕宽度的 dimens 文件。

---

### 2. 模块划分与核心代码

#### [NEW] `:core` 模块
- `BaseActivity.kt`：**Activity 基类**。封装 ViewBinding，提供沉浸式状态栏与全屏设置。
- `BaseMviViewModel.kt`：**ViewModel 基类**。统一管理 ViewState 和 ViewEffect，封装协程作用域。
- `UserPreferences.kt`：改用 **MMKV** 代替 SharedPreferences 存储 UUID 和关卡数据。
- `LogUtil.kt`：封装 **Logcat** 用于统一日志打印。
- `GsonUtil.kt`：用于 JSON 解析 and 序列化。
- `drawable/`：放置 12 张高清 `.webp` 卡牌贴图与 `ic_launcher.webp` 应用图标。
- `values/dimens.xml`：基础适配 dimens 尺寸库（包含从 1dp 到 360dp 对应的 dimens，由 ScreenMatch 进行分辨率缩放）。

#### [NEW] `:feature_splash` 模块
- `SplashActivity.kt`：启动页面。使用 **XXPermissions** 申请网络权限（虽然网络是默认权限，为合规动态申请做测试），使用 **Toaster** 提示用户，并用 **TheRouter** 路由到大厅 `/menu/main`。

#### [NEW] `:feature_menu` 模块
- `MenuActivity.kt`：主菜单大厅。提供关卡选择和昵称修改。通过 **TheRouter** 启动 `/game/play?levelId=X` 或 `/leaderboard/show?levelId=X`。

#### [NEW] `:feature_game` 模块
- `GameActivity.kt`：核心消除界面。使用 Compose 渲染。图片贴图改为用 **Coil** 加载本地的 webp 卡牌资源（`tile_1.webp` 至 `tile_12.webp`）。

#### [NEW] `:feature_leaderboard` 模块
- `LeaderboardActivity.kt`：排行榜界面。外层为 **SmartRefreshLayout**，内层为 **RecyclerView**，适配器使用 **BaseRecyclerViewAdapterHelper (BRVAH)**。
- 页面中间在加载时使用 **Lottie** 播放国风祥云或太极动画。

#### [MODIFY] `:app` 壳模块
- `MyApplication.kt`：初始化 `MMKV`、`Toaster`、`Logcat`、`TheRouter`。作为主入口并依赖所有 feature 模块。

---

## 实施步骤与任务分解

### 任务 1：全局依赖管理与 settings.gradle.kts 配置

- [ ] **步骤 1：修改 `settings.gradle.kts`**
  - 添加 JitPack 仓库支持：`maven { url = uri("https://jitpack.io") }`。
  - 移除对仓库依赖的限制，以便 JitPack 能正确解析库。
  - 声明多子模块：
    ```kotlin
    include(":app", ":core", ":feature_splash", ":feature_menu", ":feature_game", ":feature_leaderboard")
    project(":app").projectDir = file("app")
    ```
- [ ] **步骤 2：配置 `libs.versions.toml`**
  新增以下库：
  - `therouter = "1.3.2"`
  - `xxpermissions = "18.6"`
  - `toaster = "12.6"`
  - `logcat = "0.4"`
  - `utilcode = "1.31.1"`
  - `mmkv = "1.3.9"`
  - `smartrefresh = "2.0.5"`
  - `brvah = "3.0.16"`
  - `lottie = "6.4.0"`
  - `coil = "2.6.0"`
  - `gson = "2.10.1"`
- [ ] **步骤 3：修改根目录 `build.gradle.kts`**
  - 引入 Kapt 和 TheRouter classpath 插件支持。

---

### 任务 2：创建 `:core` 模块并开发通用基础库

- [ ] **步骤 1：创建 `core` 文件夹及 build.gradle.kts**
  配置成 Android Library 模块，依赖 `libs.mmkv`, `libs.gson`, `libs.utilcode`。
- [ ] **步骤 2：使用 MMKV 重构 `UserPreferences.kt`**
  - 弃用 SharedPreference，采用 `MMKV.defaultMMKV()` 实现本地存储读写。
- [ ] **步骤 3：准备 12 张 Webp 卡牌图片与应用图标**
  - 使用 `generate_image` 生成一个精致的 App 图标 `app_icon.webp`。
  - 准备 12 张高清晰的 `.webp` 国风卡牌图片（如灯笼、红包等），放入 `core/src/main/res/drawable/` 目录。
- [ ] **步骤 4：配置 `LogUtil.kt` (Logcat)**
  - 使用 `logcat` 日志库封装轻量级日志工具类。

---

### 任务 3：开发 `:feature_splash` 启动模块 (XXPermissions & Toaster)

- [ ] **步骤 1：创建 `feature_splash` 文件夹及 build.gradle.kts**
  - 引入 `TheRouter` 编译器插件和 `kapt` 处理器。
- [ ] **步骤 2：编写 `SplashActivity.kt` 逻辑**
  - 使用 `@Route(path = "/splash/entry")` 声明路由。
  - 使用 **XXPermissions** 申请运行时基本权限，失败时使用 **Toaster** 给出警告。
  - 权限通过后，通过 **TheRouter** 路由到 `/menu/main` 大厅并 `finish()` 当前页面。

---

### 任务 4：开发 `:feature_menu` 大厅模块

- [ ] **步骤 1：创建 `feature_menu` 文件夹及 build.gradle.kts**
  - 声明依赖 `:core`。
- [ ] **步骤 2：实现 `MenuActivity.kt` 路由与界面**
  - 使用 `@Route(path = "/menu/main")` 声明路由。
  - 点击开始游戏，使用 `TheRouter.build("/game/play").withInt("levelId", levelId).navigation()` 路由至消除页。
  - 点击排行，路由至 `/leaderboard/show` 排行榜页面。

---

### 任务 5：开发 `:feature_game` 消除模块 (Coil 加载 Webp)

- [ ] **步骤 1：创建 `feature_game` 文件夹及 build.gradle.kts**
  - 声明依赖 `:core`。
- [ ] **步骤 2：开发 `GameActivity.kt` 游戏界面**
  - 使用 `@Route(path = "/game/play")` 声明路由，在 `onCreate` 中通过 `intent.getIntExtra("levelId")` 获取当前关卡。
- [ ] **步骤 3：修改 `TileView` 采用 Coil 加载 Webp 图片**
  - 废弃原 XML Vector 图案，卡片渲染部分使用 **Coil** 的 `SubcomposeAsyncImage` 或 `AsyncImage` 来加载本地的 webp drawable 资源。

---

### 任务 6：开发 `:feature_leaderboard` 排行榜模块 (SmartRefreshLayout + BRVAH + Lottie)

- [ ] **步骤 1：创建 `feature_leaderboard` 模块并配置 layout XML**
  - 创建 `activity_leaderboard.xml` 包含 `SmartRefreshLayout` 和内嵌的 `RecyclerView`。
  - 在页面中心添加 **LottieAnimationView** 播放加载动画。
- [ ] **步骤 2：开发 `LeaderboardAdapter` 适配器 (BRVAH)**
  - 继承 `BaseQuickAdapter`，绑定排行榜实体，快速渲染玩家头像、昵称、用时和达成时间。
- [ ] **步骤 3：在 `LeaderboardActivity.kt` 实现下拉刷新逻辑**
  - 使用 `@Route(path = "/leaderboard/show")` 声明路由。
  - 绑定 **SmartRefreshLayout** 的 `onRefreshListener`。当刷新触发时，发起网络请求更新数据，更新后调用 `finishRefresh()`。
  - 数据加载时开启 Lottie 动画，加载完毕后隐藏 Lottie 并显示列表。

---

### 任务 7：重构主 `:app` 壳模块

- [ ] **步骤 1：重构 `MainActivity.kt`**
  - 将启动逻辑移交给 `:feature_splash`，`MainActivity` 仅作为占位，或者将其声明为路由跳转桥接。
- [ ] **步骤 2：创建 `MyApplication.kt` 并初始化库**
  - 初始化 **MMKV** (`MMKV.initialize(this)`)。
  - 初始化 **Toaster** (`Toaster.init(this)`)。
  - 配置 **TheRouter** 的编译与动态初始化。
- [ ] **步骤 3：在 settings.gradle.kts 中依赖所有模块，运行打包验证**
  - 确认跨模块路由跳转顺畅，各第三方 SDK 均能正常工作。

---

## 验证计划

### 编译验证
- 运行 `./gradlew assembleDebug`，验证所有模块编译通过，TheRouter 成功生成路由表。

### 交互验证
1. **启动测试**：启动后，展示 Splash，调用 XXPermissions 弹出权限申请，并展示 Toaster。
2. **大厅跳转**：同意权限后自动通过 TheRouter 路由到大厅 Activity。
3. **消除盘 Coil 渲染**：点击第一关，进入 GameActivity，检查卡片图案是否是高清 Webp 图片且正确被 Coil 加载。
4. **排行榜刷新与动画**：点击排行，进入 LeaderboardActivity，祥云 Lottie 动画应播放，SmartRefreshLayout 下拉可以刷新，RecyclerView 能通过 BRVAH 渲染出成绩。
