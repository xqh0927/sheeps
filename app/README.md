# Sheeps - MVI 多模块联机手游项目文档

## 1. 项目简介
**Sheeps** 是一款基于仙侠主题背景的益智类三消手游。本项目不仅实现了单机闯关模式，还集成了基于 WebSocket 的实时多人对战系统（天命对决），并包含完整的商城、背包、任务、排行及离线同步功能。

项目采用最新的 Android 开发标准，深度实践了 **MVI (Model-View-Intent)** 架构、**Jetpack Compose** 响应式 UI 以及 **多模块化 (Multi-Module)** 工程结构。

---

## 2. 技术栈
### 核心架构
- **MVI 模式**：通过单向数据流 (Unidirectional Data Flow) 管理 UI 状态，确保状态的可预测性与可测试性。
- **Multi-Module**：按功能拆分模块，提升编译速度与代码隔离度。
- **Hilt (Dependency Injection)**：全项目依赖注入，解耦各层级逻辑。

### 视图层 (UI)
- **Jetpack Compose**：全申明式 UI 开发。
- **Coil**：图片加载库。
- **Compose Animation**：丰富的卡牌飞行动画与界面切换效果。

### 数据层 (Data)
- **Room Persistence**：本地 SQLite 数据库存储（用户信息、关卡进度、道具库存）。
- **MMKV**：高性能 KV 存储，用于用户偏好设置（主题、语言、UID）。
- **Retrofit + OkHttp**：RESTful API 通信。
- **Kotlin Serialization**：轻量级、高性能的 JSON 序列化。

### 联机与异步
- **WebSocket**：实时全双工对战指令同步。
- **Kotlin Coroutines & Flow**：响应式异步编程。
- **WorkManager**：保证离线成绩与数据的最终一致性同步。

---

## 3. 项目模块说明
项目按业务和功能划分为以下主要模块：

| 模块名 | 职能描述 |
| :--- | :--- |
| `:app` | 空壳入口模块。初始化 Application，配置 WorkManager，管理全局生命周期。 |
| `:core` | **核心公共库**。包含 MVI 基类、主题管理、网络监控、数据库实体、协议模型及全局工具类。 |
| `:feature_menu` | **主菜单模块**。包含游戏大厅、道具商城、个人中心、签到任务、在线匹配逻辑。 |
| `:feature_game` | **游戏核心模块**。包含单机闯关 (GameScreen) 与 多人对决 (DuelScreen) 的核心逻辑与 UI。 |
| `:feature_leaderboard` | **排行榜模块**。展示全球及好友之间的关卡成绩排名。 |
| `:feature_splash` | **启动模块**。处理 App 启动图、初始化校验及路由分发。 |

---

## 4. 核心功能实现细节

### 4.1 游戏引擎 (GameEngine)
位于 `:core` 模块，负责卡牌的遮挡算法：
- **遮挡判定**：基于 3D 坐标 (X, Y, Z)，通过计算卡牌位置重叠度，动态判定卡牌是否被“压住”。
- **状态刷新**：每当一张卡牌被点击移入槽位，引擎会自动递归计算下方受影响卡牌的锁定状态。

### 4.2 联机对战系统 (Duel Mode)
- **实时同步**：使用 WebSocket 建立持久连接。
- **能量系统**：玩家消除卡牌会积累能量，能量可用于释放“迷雾咒”、“封印咒”等干扰对手。
- **退避重连**：`WebSocketManager` 内置了指数退避算法，应对不稳定的移动网络环境。

### 4.3 离线优先同步策略 (SyncRepository)
- **Dirty 标记位**：本地数据库中的数据（如积分、进度）带有 `isDirty` 字段。
- **静默同步**：当应用检测到网络恢复在线，或应用前后台切换时，会自动触发 `SyncRepository` 将本地脏数据同步至云端。

### 4.4 ViewModel 委派模式
为了解决大型页面 ViewModel 逻辑过于复杂的问题，本项目实践了 **Delegate (委派)** 模式：
- `GameViewModel` 仅负责分发 Intent。
- 具体的业务逻辑被剥离到 `GameLogicDelegate` (消除判定)、`GameToolDelegate` (道具使用) 等类中，显著提升了可读性。

---

## 5. 项目结构导航
```text
com.example.sheeps
├── core
│   ├── base        # Activity/ViewModel 基类
│   ├── game        # 游戏核心算法引擎
│   ├── multiplayer # WebSocket 管理与对战协议
│   ├── theme       # 仙侠风格主题管理系统
│   └── utils       # 网络监控、语言工具、事件总线
├── data
│   ├── local       # Room 数据库配置与 DAO
│   ├── model       # 统一的业务数据模型 (MVI State/Intent)
│   ├── network     # Retrofit 接口定义
│   └── repository  # 数据仓库与云端同步逻辑
└── feature_xxx     # 各功能模块
    ├── ui
    │   ├── components # 拆分出的功能卡片组件
    │   ├── dialogs    # 业务相关的弹窗
    │   └── screens    # 模块主界面
    └── viewmodel
        └── delegates  # 业务逻辑委派实现类
```

---

## 6. 开发与构建
- **环境要求**：Android Studio Jellyfish+ / AGP 8.0+。
- **Gradle 命令**：
    - `assembleDebug`：构建调试版 APK。
    - `check`：执行静态代码扫描。
- **路由系统**：项目使用 `TheRouter` 进行模块间跳转，支持动态下发路由。
