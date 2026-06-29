# 秘境消消乐 - 游戏功能优化与无限关卡设计方案

本设计文档旨在解决秘境消消乐手游中存在的关卡解锁丢失、主题切换缺失与写死、导航图标颜色不随主题切换以及不支持无限关卡的问题。

## 1. 关卡解锁丢失与同步优化方案

### 问题分析
1. 玩家在 `GameActivity` 中通关后，`GameViewModel` 的 `submitScoreOnline` 通过 `viewModelScope` 异步发起云端分数提交。如果玩家立即返回主菜单，`GameActivity` 被销毁，`viewModelScope` 会被取消，导致云端分数提交请求终止，云端并未记录新关卡解锁。
2. 返回 `MenuActivity` 后，在 `onResume` 中触发 `LoadData`，从而执行 `pullCloudProfile`。由于云端无最新关卡解锁记录，且 `pullCloudProfile` 会清空本地 Progress 库并以云端最新数据覆盖，从而把本地保存的下一关解锁状态抹除，导致关卡显示为锁定。

### 优化方案
1. **全局独立协程范围**：在 `GameViewModel` 的分数提交中使用 `CoroutineScope(Dispatchers.IO).launch` 来发起网络请求与本地保存，使请求生命周期独立于 ViewModel 的生命周期，确保即便 Activity 销毁，通关记录也能安全写入本地 Room 数据库并提交至云端。
2. **同步优先加载**：在 `MenuViewModel.kt` 的 `handleLoadData()` 中，在执行 `pullCloudProfile()` 覆盖本地数据库之前，**先执行** `syncRepository.syncDirtyData()`。这确保本地离线通关的脏数据能够率先成功推送到云端，合并后再拉取最新云端状态，杜绝覆盖丢失。

---

## 2. 默认浅色主题与动态主题切换方案

### 问题分析
1. 默认主题被写死为暗色系 `AppTheme.MO_YE_GOLD`。
2. 缺乏切换主题的 UI 入口。
3. 主界面、对话框、按钮等组件大量直接引用 `MoYe_Background`、`MoYe_Surface` 等暗色系硬编码颜色，无法跟随主题自适应。

### 优化方案
1. **调整默认主题**：将 `ThemeManager.kt` 中的默认主题修改为浅色系国风主题 `AppTheme.QING_RI_CHUN`。
2. **新增主题切换 UI**：在 `PersonalTabContent`（“我的” Tab）中，新增“主题设置”卡片，提供“清日春(浅色)”、“墨夜金(金黑)”、“暗黑(深黑)”三档切换，切换时调用 `ThemeManager.setTheme` 并触发 Activity 的 `recreate()` 以确保 XML 属性重新加载。
3. **消除硬编码颜色**：将 Compose 代码中硬编码的 `MoYe_` 背景、卡片、文字及边框颜色替换为 `MaterialTheme.colorScheme` 对应的动态属性（如 `background`、`surface`、`surfaceVariant`、`outline` 等）。

---

## 3. 图标颜色 `iconColor` 与主题自适应方案

### 问题分析
1. 底部导航图标 `ic_nav_game.xml` 等在 XML 中引用了 `?attr/iconColor`。但因为 Activity 使用的 XML 主题是静态声明的，运行时更换 Compose 主题时 XML drawable 无法感知。
2. 底部导航栏当前使用 Material Icons 默认图标，而非这套专用的国风 XML 矢量图。

### 优化方案
1. **声明多套 XML 主题**：在 `core/src/main/res/values/themes.xml` 中声明对应的 XML 主题（`Theme.Sheeps.MoYeGold`、`Theme.Sheeps.QingRiChun`、`Theme.Sheeps.DarkMode`），分别定义 `iconColor` 颜色属性。
2. **Activity 级别主题绑定**：在 `BaseActivity.kt` 中，在 `super.onCreate()` 之前调用 `setTheme(ThemeManager.getThemeResId())`，使 Activity 在加载布局时应用正确的 XML 主题属性。
3. **底部导航替换**：将 `MenuBottomNavigation` 替换为自定义的 `ic_nav_game`、`ic_nav_shop`、`ic_nav_profile`。使用 Compose 的 `painterResource` 加载，依靠 `NavigationBarItem` 自动进行 Tint 染色以支持动态高亮与变灰。

---

## 4. 无限关卡生成算法方案

### 数学公式
随着关卡号 $x$ 递增：
1. **图案种类 $T$（对数增长，上限 16）**：
   $$T(x) = \begin{cases} 3, & x = 1 \\ \min(16, \lfloor 3 + 3 \ln(x) \rfloor), & x > 1 \end{cases}$$
2. **总牌组 $N$（线性递增，每组3张对齐）**：
   $$N(x) = \begin{cases} 9, & x = 1 \\ 36, & x = 2 \\ 36 + (x - 2) \times 12, & x > 2 \end{cases}$$
3. **堆叠层数 $L$（平方根递减平滑收敛，上限 12）**：
   $$L(x) = \begin{cases} 3, & x = 1 \\ 4, & x = 2 \\ \min\left(12, \left\lfloor 12 - \frac{8}{\sqrt{x - 1}} \right\rfloor\right), & x > 2 \end{cases}$$

### 客户端与服务端对齐
1. **服务端** 已部分按此实现，将确保 `/api/score/submit` 的自动解锁能顺畅递增到无限关卡（解锁 `levelId + 1`）。
2. **本地客户端**：修改 `GameViewModel.kt` 的 `generateSolvableLevelLocal` 算法，对齐上述数学公式，以确保离线/弱网降级模式下生成的关卡数据与服务端完全确定一致。
3. **UI 无限展示**：修改 `MenuActivity.kt` 关卡列表数据源，将原本写死的 `1..20` 改为 `1..maxOf(20, state.unlockedLevel + 5)`，随着玩家不断通关，关卡列表自动延长，支持无限向下游玩。
