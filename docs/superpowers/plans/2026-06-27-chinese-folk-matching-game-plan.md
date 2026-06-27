# 国风民俗消除小游戏（类羊了个羊）实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标**：构建一个国风民俗主题的层叠消除游戏，Android 端使用 Kotlin + Jetpack Compose (MVI 架构) 开发，后端部署在 Cloudflare Workers (TypeScript) 并连接 D1 SQL 数据库，支持高画质图标、特殊牌机制（盲盒卡/封印卡）以及云端排行榜。

**架构**：Android 客户端发起网络请求连接 Cloudflare Worker APIs，存储和读取关卡坐标与排行榜得分。客户端以 MVI 模式管理 UI 状态，游戏界面基于 Compose Canvas 和 Modifier 动画，完全脱离传统 XML Views。

**技术栈**：Kotlin, Jetpack Compose, Material 3, ViewModel, StateFlow, Retrofit, Cloudflare Workers, TypeScript, Cloudflare D1 (SQLite), Wrangler.

---

## Proposed Changes

### 后端模块 (Cloudflare Worker & D1 Database)

#### [NEW] [wrangler.toml](file:///c:/Users/15613/Documents/file/server/wrangler.toml)
Cloudflare Workers 的配置文件，包含 D1 数据库绑定。

#### [NEW] [schema.sql](file:///c:/Users/15613/Documents/file/server/schema.sql)
D1 数据库建表 SQL（用户表、关卡表、排行榜表）。

#### [NEW] [index.ts](file:///c:/Users/15613/Documents/file/server/src/index.ts)
后端主入口，基于 Cloudflare Workers 实现 RESTful APIs，处理数据增删改查及简单的加密签名校验。

---

### Android 客户端模块 (MVI 架构)

#### [NEW] [build.gradle.kts](file:///c:/Users/15613/Documents/file/app/build.gradle.kts)
Android 项目主 Gradle 构建配置，声明 Compose、Retrofit、Serialization 和 Coroutines 依赖。

#### [NEW] [AndroidManifest.xml](file:///c:/Users/15613/Documents/file/app/src/main/AndroidManifest.xml)
配置应用基本信息，仅声明 `INTERNET` 权限，设置 `SplashActivity` 为入口。

#### [NEW] [GameModels.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/data/model/GameModels.kt)
包含关卡配置、卡片状态、用户及排行等数据实体定义。

#### [NEW] [UserPreferences.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/data/preference/UserPreferences.kt)
保存设备的唯一 ID (UUID) 和隐私协议签署状态。

#### [NEW] [ApiService.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/data/network/ApiService.kt)
Retrofit 客户端，用于异步与 Cloudflare Worker 进行 HTTPS 请求。

#### [NEW] [GameMviContract.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/ui/state/GameMviContract.kt)
定义 MVI 架构下的 `GameViewState`（只读状态）、`GameViewIntent`（用户意图）和 `GameViewEffect`（单次副作用）。

#### [NEW] [GameViewModel.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/viewmodel/GameViewModel.kt)
核心 ViewModel，处理全部游戏核心逻辑：叠层关系重算、特殊牌逻辑（盲盒与解封）、关卡生成器、道具使用逻辑、网络通讯及签名提交。

#### [NEW] [Theme.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/ui/theme/Theme.kt)
配置国风民俗配色主题（宫墙红、帝王黄、水墨灰等）。

#### [NEW] [SplashActivity.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/ui/screens/SplashActivity.kt)
符合合规的启动页，展示健康游戏忠告和 CADPA 8+ 适龄绿标。

#### [NEW] [PrivacyDialog.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/ui/components/PrivacyDialog.kt)
工信部合规标准隐私政策协议弹窗，拒绝则优雅退出，同意则进入主菜单并允许初始化网络。

#### [NEW] [MenuScreen.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/ui/screens/MenuScreen.kt)
主菜单界面，国风卷轴拉开的关卡选择与排行榜入口。

#### [NEW] [GameScreen.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/ui/screens/GameScreen.kt)
核心游戏界面，使用 Canvas 和 Modifier 动画渲染重叠卡牌、卡槽、道具栏及关卡胜利/失败遮罩。

#### [NEW] [LeaderboardScreen.kt](file:///c:/Users/15613/Documents/file/app/src/main/java/com/game/sheepsheep/ui/screens/LeaderboardScreen.kt)
展示当前关卡的全球前 50 名速度排行榜。

---

## 实施步骤与任务分解

### 任务 1：搭建 Cloudflare Worker 后端与 D1 数据库

**文件**：
* 创建：`server/wrangler.toml`
* 创建：`server/schema.sql`
* 创建：`server/src/index.ts`

- [ ] **步骤 1：创建 `wrangler.toml` 文件**
  配置 Worker 的基础配置并声明 D1 数据库绑定。
- [ ] **步骤 2：创建 `schema.sql` 数据库结构**
  定义 `users`、`levels` 和 `leaderboard` 表。
- [ ] **步骤 3：编写 TypeScript 服务逻辑 `index.ts`**
  实现注册、重命名、获取关卡、排行榜查询和分数提交，并在提交分数时进行防作弊简单校验（校验分值、时间与生成的 MD5/Hmac 签名）。
- [ ] **步骤 4：本地模拟测试 API 连通性**
  使用 `wrangler dev` 启动本地 D1，使用 http-client 工具或 `curl` 测试 API，确认返回正确的 JSON 数据。

---

### 任务 2：初始化 Android 项目与基础依赖

**文件**：
* 创建：`app/build.gradle.kts`
* 创建：`app/src/main/AndroidManifest.xml`
* 创建：`app/src/main/java/com/game/sheepsheep/ui/theme/Theme.kt`

- [ ] **步骤 1：生成 Android Compose 项目骨架**
  使用 `android create empty-activity --name="Sheeps" --output=./app` 初始化项目。
- [ ] **步骤 2：配置 `build.gradle.kts`**
  添加 Retrofit、Serialization、Coroutines、Compose-navigation、ViewModel 依赖。
- [ ] **步骤 3：声明 Manifest**
  仅声明 `android.permission.INTERNET`，设置 SplashActivity 为入口，开启全屏沉浸布局支持。
- [ ] **步骤 4：定义国风 Color Palette**
  在 `Theme.kt` 中声明 `CrimsonRed` `#C82423`, `ImperialYellow` `#E6A23C`, `PaperWhite` `#F5F5F7` 主题色。

---

### 任务 3：编写合规组件（启动页与隐私弹窗）

**文件**：
* 创建：`app/src/main/java/com/game/sheepsheep/data/preference/UserPreferences.kt`
* 创建：`app/src/main/java/com/game/sheepsheep/ui/components/PrivacyDialog.kt`
* 创建：`app/src/main/java/com/game/sheepsheep/ui/screens/SplashActivity.kt`

- [ ] **步骤 1：编写本地 SharedPreference 存储 `UserPreferences.kt`**
  保存用户是否已同意隐私协议，以及设备生成唯一 UUID。
- [ ] **步骤 2：实现 `PrivacyDialog` 隐私弹窗**
  包含醒目的标题、协议文本、可点击至网页的协议链接（用户协议与隐私条款），以及“同意”和“不同意（退出）”两个按钮。
- [ ] **步骤 3：实现 `SplashActivity` 启动页**
  展示传统的中国健康游戏忠告、CADPA 适龄绿标。在展示 1.5s 后检测隐私是否同意：若同意，则进入主页；若未同意，则弹出隐私政策弹窗。

---

### 任务 4：定义 MVI 状态流与网络层

**文件**：
* 创建：`app/src/main/java/com/game/sheepsheep/data/model/GameModels.kt`
* 创建：`app/src/main/java/com/game/sheepsheep/data/network/ApiService.kt`
* 创建：`app/src/main/java/com/game/sheepsheep/ui/state/GameMviContract.kt`

- [ ] **步骤 1：定义基本实体类 `GameModels.kt`**
  定义 `Tile` 卡片数据模型（包括坐标、Z层级、卡片状态、特殊属性如盲盒和封印等）、关卡布局、得分等数据类。
- [ ] **步骤 2：编写 `ApiService.kt` 请求接口**
  声明用于获取关卡、注册 and 提交/拉取排行榜的 Retrofit 挂起函数。
- [ ] **步骤 3：编写 MVI 契约 `GameMviContract.kt`**
  声明只读的 `GameViewState`（包含 `boardTiles`、`slotTiles`、`undoStack`、`remainingItems`、`currentScore` 等）以及 `GameViewIntent` 和 `GameViewEffect`。

---

### 任务 5：实现核心层叠遮挡与逆向关卡生成逻辑

**文件**：
* 创建：`app/src/main/java/com/game/sheepsheep/viewmodel/GameViewModel.kt`

- [ ] **步骤 1：编写重合判定与遮挡计算逻辑**
  在 ViewModel 中根据卡片 2D 尺寸与 3D 坐标 $(x, y, z)$，动态重算每一张卡片的被遮挡状态 (`BLOCKED` / `NORMAL`)。
- [ ] **步骤 2：编写“保证可解”的逆向关卡生成算法**
  实现关卡算法：根据关卡模板的空位坐标集，从顶部没有被任何卡牌遮挡的暴露空位出发，逆向每次随机取出 3 个空位分配同一图案，并在已占用的数据结构中记录它们，循环直到全部分配完毕。
- [ ] **步骤 3：实现特殊牌（盲盒与封印）判定**
  - 如果卡牌是盲盒卡，在被遮挡时将展示图案强置为问号，当解封时翻转显示真实图案。
  - 如果卡牌是封印卡，点击时首先消耗一层封印值（并从封印列表移除），而不进入卡槽。
- [ ] **步骤 4：实现道具操作**
  - 移出：将槽中前3张牌剪切至 `movedOutTiles`。
  - 撤销：将上一次进入卡槽的操作出栈，并弹回桌面。
  - 洗牌：打乱所有剩余卡片的 `type` 重新计算遮挡。

---

### 任务 6：下载网页清晰图片资源

**文件**：
* 新增：`app/src/main/res/drawable-xxhdpi/` 目录下的 12 张卡牌国风图片资源。

- [ ] **步骤 1：编写脚本或直接在网页下载 12 张高清晰度透明国风 PNG 图标**
  获取 12 种图案：红灯笼、红包袋、青花瓷、折扇、中国结、铜钱、醒狮头、月饼、紫砂壶、鞭炮、锦鲤、仙桃。
- [ ] **步骤 2：规范命名并导入 drawable**
  将获取 of PNG 统一缩放到 $128 \times 128$ 像素或以上，以确保在屏幕上显示清晰。重命名为 `tile_lantern.png`, `tile_porcelain.png` 等导入 `drawable-xxhdpi` 目录。
- [ ] **步骤 3：准备好卡牌背景与边框的 XML/SVG 资源**
  创建一个高质感的金色浮雕边框背景 `tile_background.xml` 用于绘制卡片底层。

---

### 任务 7：构建 Jetpack Compose 游戏页面与动画渲染

**文件**：
* 创建：`app/src/main/java/com/game/sheepsheep/ui/screens/MenuScreen.kt`
* 创建：`app/src/main/java/com/game/sheepsheep/ui/screens/GameScreen.kt`
* 创建：`app/src/main/java/com/game/sheepsheep/ui/screens/LeaderboardScreen.kt`

- [ ] **步骤 1：编写国风菜单 `MenuScreen`**
  实现带有传统卷轴样式的关卡列表，并显示当前已通关分数。
- [ ] **步骤 2：绘制 `GameScreen` 游戏棋盘**
  使用 Compose `Box` 并根据每张卡片的 `(x, y, z)` 将其渲染到对应的绝对位置（通过 `Modifier.graphicsLayer` 转换）。
- [ ] **步骤 3：实现点击卡牌飞入卡槽的平滑动画**
  利用 Compose 的 `Animatable` 或 `updateTransition` 控制卡片从桌面坐标飞入下方消除卡槽（并在移动到卡槽后触发归类合并动画）。
- [ ] **步骤 4：完成排行榜页 `LeaderboardScreen`**
  拉取云端 D1 数据库中的前 50 名通关时间数据，使用优雅的列表（如前三名带有金/银/铜边框）展示。

---

## 验证计划

### 自动化验证
* 在 Android 中编写单元测试类 `LayerCalculationTest.kt`，验证对各种重叠坐标的遮挡状态判定是否 100% 正确。
* 编写 `LevelSolvabilityTest.kt`，编写一个模拟玩家（优先消除无遮挡卡牌且凑 3 个）的决策脚本，验证逆向关卡生成器生成的关卡是否确实能被 100% 解开。

### 手动验证
1. 启动 App 后，确认隐私政策协议弹窗首先显示。点击“不同意”，应用应立即退出；再次启动，点击“同意”，App 才能加载主页。
2. 运行本地 Worker 后端，启动 App 玩一局第一关，确认能够顺利消除，通关后提交成绩并在排行榜上查看到自己的数据。
3. 检查卡片在全面屏上的分辨率是否足够高，动画是否流畅不卡顿。
