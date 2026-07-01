# Unity 羊了个羊游戏项目

## 项目简介

这是一个使用Unity引擎重新实现的"羊了个羊"类型消除游戏。原项目是Android原生开发（Kotlin/Compose），现在使用Unity和C#重新实现。

## 项目结构

```
UnityGame/
├── Assets/
│   ├── Scripts/
│   │   ├── Core/              # 核心游戏逻辑
│   │   │   ├── GameEngine.cs          # 游戏核心引擎（遮挡计算）
│   │   │   └── GameLevelGenerator.cs  # 关卡生成器
│   │   ├── Data/              # 数据模型
│   │   │   ├── Tile.cs               # 卡牌数据模型
│   │   │   ├── TileState.cs          # 卡牌状态枚举
│   │   │   └── GameLevel.cs          # 关卡和槽位数据
│   │   ├── Game/              # 游戏逻辑
│   │   │   └── GameManager.cs        # 游戏主管理器
│   │   └── UI/               # UI组件
│   │       ├── TileView.cs           # 卡牌视图组件
│   │       ├── GameBoard.cs          # 游戏棋盘组件
│   │       ├── SlotUI.cs             # 槽位UI组件
│   │       └── GameUI.cs            # 游戏UI管理器
│   ├── Prefabs/              # 预制体
│   ├── Scenes/               # 场景文件
│   ├── Sprites/              # 卡牌图片资源
│   └── Animations/          # 动画文件
└── README.md                # 本文件
```

## 核心功能实现

### 1. 游戏核心引擎 (GameEngine.cs)

对应原Android项目的 `GameEngine.kt`，负责：

- **IsTileBlocked()** - 判断卡牌是否被遮挡
- **GetBlockingTiles()** - 获取遮挡某张牌的所有卡牌
- **CalculateBlockedStates()** - 批量计算并更新棋盘卡牌状态

**遮挡算法**：
- 根据卡牌的 (x, y) 坐标和 z 轴层级计算重叠面积
- 重叠面积 > 230.4（约半张牌面积）时判定为被遮挡

### 2. 关卡生成器 (GameLevelGenerator.cs)

对应原Android项目的 `GameLevelGenerator.kt`，负责：

- 生成可解的关卡（卡牌数量是3的倍数）
- 根据关卡ID调整难度（卡牌数量、层数）
- 确保每种类型的卡牌数量都是3的倍数

### 3. 游戏管理器 (GameManager.cs)

对应原Android项目的 `GameViewModel.kt`，负责：

- 游戏状态管理（菜单、游戏中、胜利、失败）
- 卡牌点击逻辑
- 槽位管理（最多7张，3张相同消除）
- 道具系统
- 分数计算

### 4. UI组件

- **TileView.cs** - 单张卡牌的显示和交互
- **GameBoard.cs** - 棋盘渲染，管理所有卡牌的位置和层级
- **SlotUI.cs** - 槽位显示，卡牌飞行动画
- **GameUI.cs** - UI管理器，按钮事件、面板切换

## 道具系统

游戏包含以下道具：

| 道具 | 功能 | 对应原Android代码 |
|------|------|-------------------|
| UNDO | 撤销上一步操作 | GameToolDelegate.handleUseUndo() |
| SHUFFLE | 洗牌（打乱棋盘卡牌位置） | GameToolDelegate.handleUseShuffle() |
| MOVEOUT | 移出槽位前3张卡牌 | GameToolDelegate.handleUseMoveOut() |
| REVIVE | 复活（失败后继续使用） | GameViewModel.handleRevive() |
| HINT | 提示（高亮可消除的卡牌） | GameToolDelegate.handleUseHint() |
| BOMB | 炸弹（移除槽位1张卡牌） | GameToolDelegate.handleUseBomb() |
| JOKER | 万能牌（变成可匹配任何类型） | GameToolDelegate.handleUseJoker() |
| DOUBLE_POINTS | 双倍积分 | GameViewModel.handleUseDoublePoints() |

## Unity场景搭建指南

### 步骤1：创建场景

1. 打开Unity Hub，创建新的Unity项目（建议使用Unity 2021.3 LTS或更高版本）
2. 将 `UnityGame` 文件夹复制到项目的 `Assets` 目录下
3. 在Unity编辑器中，右键点击 `Assets/Scenes` 文件夹，选择 `Create > Scene`，命名为 `GameScene`

### 步骤2：搭建UI

1. **创建Canvas**：
   - 右键点击Hierarchy面板，选择 `UI > Canvas`
   - 设置Canvas的Render Mode为 `Screen Space - Camera`
   - 添加 `Canvas Scaler` 组件，设置UI Scale Mode为 `Scale With Screen Size`，参考分辨率设置为 `1080 x 1920`（竖屏）

2. **创建游戏棋盘 (GameBoard)**：
   - 在Canvas下创建空GameObject，命名为 `GameBoard`
   - 将 `GameBoard.cs` 脚本拖到 `GameBoard` 对象上
   - 创建卡牌预制体（见步骤3）
   - 将卡牌预制体赋值给 `GameBoard` 的 `Tile Prefab` 字段

3. **创建槽位 (SlotUI)**：
   - 在Canvas下创建空GameObject，命名为 `SlotContainer`
   - 将 `SlotUI.cs` 脚本拖到 `SlotContainer` 对象上
   - 设置 `Slot Container` 为自身Transform
   - 创建槽位卡牌预制体（可以与棋盘卡牌相同）

4. **创建游戏UI管理器 (GameUI)**：
   - 在Canvas下创建空GameObject，命名为 `GameUI`
   - 将 `GameUI.cs` 脚本拖到 `GameUI` 对象上
   - 在Inspector中赋值所有UI引用（面板、按钮、文本等）

### 步骤3：创建卡牌预制体

1. 在Hierarchy面板中右键点击，选择 `UI > Button`，命名为 `TilePrefab`
2. 删除Button组件上的 `Text (Legacy)` 子对象
3. 添加 `Image` 组件作为卡牌图片
4. 可选：添加 `TileView.cs` 脚本（如果不通过GameBoard动态添加）
5. 将 `TilePrefab` 拖到 `Assets/Prefabs` 文件夹中，保存为预制体
6. 删除Hierarchy中的 `TilePrefab`

### 步骤4：创建游戏管理器

1. 在Hierarchy面板中创建空GameObject，命名为 `GameManager`
2. 将 `GameManager.cs` 脚本拖到 `GameManager` 对象上
3. 确保 `GameManager` 对象在场景中存在且唯一

### 步骤5：配置引用

1. 选中 `GameUI` 对象
2. 在Inspector面板中，将以下对象拖到对应字段：
   - `Menu Panel` - 主菜单面板
   - `Game Panel` - 游戏界面面板
   - `Win Panel` - 胜利面板
   - `Lose Panel` - 失败面板
   - `Score Text` - 分数文本（TextMeshProUGUI）
   - `Level Text` - 关卡文本
   - `Slot UI` - SlotUI组件
   - `Game Board` - GameBoard组件
   - 所有道具按钮和数量文本

3. 配置道具初始数量（在 `GameManager` 的Inspector中设置）

### 步骤6：测试游戏

1. 点击Unity编辑器的 `Play` 按钮
2. 游戏应该会加载第1关
3. 点击卡牌进行测试
4. 测试道具功能

## 资源需求

### 卡牌图片

你需要在 `Assets/Sprites` 文件夹中准备卡牌图片资源：

- 至少12种不同的卡牌图案（对应 `TOTAL_TILE_TYPES = 12`）
- 建议尺寸：256x256 或 512x512 像素
- 格式：PNG（支持透明背景）

###  UI素材

- 棋盘背景图片
- 按钮正常/按下/禁用状态的图片
- 面板背景图片
- 道具图标

## 与原Android项目的对应关系

| Unity脚本 | 原Android文件 | 功能描述 |
|-----------|---------------|----------|
| GameEngine.cs | GameEngine.kt | 游戏核心引擎，处理卡牌遮挡逻辑 |
| GameLevelGenerator.cs | GameLevelGenerator.kt | 关卡生成器 |
| GameManager.cs | GameViewModel.kt | 游戏主逻辑ViewModel |
| TileView.cs | TileView.kt (Compose) | 卡牌视图组件 |
| GameBoard.cs | GameBoard.kt (Compose) | 游戏棋盘Composable |
| GameUI.cs | GameScreen.kt (Compose) | 游戏界面Screen |
| SlotUI.cs | GameDock.kt (Compose) | 槽位Composable |

## 待完善功能

以下功能在原Android项目中有实现，但在当前Unity版本中还需要完善：

1. **网络功能**：
   - 排行榜（Leaderboard）
   - 分数上传
   - 用户注册/登录

2. **本地化**：
   - 多语言支持（中文、英文、日文、韩文）

3. **音效和动画**：
   - 卡牌点击音效
   - 消除动画
   - 道具使用动画

4. **皮肤系统**：
   - 卡牌皮肤切换
   - 棋盘主题

5. **决斗模式**：
   - 双人对战
   - 实时匹配

## 开发建议

1. **使用版本控制**：建议使用Git进行版本控制
2. **测试不同分辨率**：在多种设备和分辨率下测试UI适配
3. **性能优化**：
   - 对象池（Object Pooling）用于卡牌预制体
   - 减少Update()的使用
   - 使用Coroutines代替Update进行动画

## 常见问题

**Q: 卡牌图片不显示？**
A: 检查 `Sprites` 文件夹中是否有图片资源，并在 `TileView.cs` 的 `GetTileSprite()` 方法中正确加载图片。

**Q: 卡牌点击没反应？**
A: 检查卡牌的 `Tile.state` 是否为 `NORMAL`，以及 `Button.interactable` 是否为 `true`。

**Q: 遮挡逻辑不正确？**
A: 检查 `GameEngine.cs` 中的遮挡算法参数（`TILE_SIZE`, `TILE_SPACING`, `BLOCK_THRESHOLD`）是否与原项目一致。

## 联系方式

如有问题或建议，请提交Issue或Pull Request。

---

**注意**：本项目是学习和研究用途，尊重原"羊了个羊"游戏的知识产权。
