# Phase 2 & 3 执行小结 · 羊了个羊 Android 结构重构

> 分支：`refactor/struct-phase23` · 提交：`547a6ce1`
> 说明：沙箱环境无法编译，以下为**已落盘并校验**的重构结果，需你在本地 Android Studio / Gradle 完成编译验证。

## 一、做了什么

### Phase 2 — 包名 / 命名空间对齐（已完成）
- `:lib_base`：`core.base` → `lib_base.base`、`core.crypto` → `lib_base.crypto`、`IGameService` → `lib_base.service`
- `:lib_network`：`core` → `lib_network`（AppConfig / EncryptionInterceptor）
- 全工程 import 替换为 0 残留

### Phase 3 — 物理拆分 `:core` → `:data` / `:designsystem` / 瘦身 `:core`
| 模块 | namespace | 内容 | 关键依赖 |
|---|---|---|---|
| `:data` | `com.example.sheeps.data` | `data/{local,model,network,repository,result}` + 下沉的 `preference` / `utils(NetworkMonitor)` / `game(SkinConstants)` | `api(:lib_network)`、`impl(:lib_base, :designsystem)`、Room、Hilt、serialization |
| `:designsystem` | `com.example.sheeps.ui` | `theme`→`ui/theme`、`ui/components`、**全部 `res/`**（R 类归属此模块） | `api(:lib_base)`、Coil |
| `:core`（瘦身） | `com.example.sheeps.core` | `core/{cache,di,game,multiplayer,update,utils}` | `api(:data)`、`api(:designsystem)`、`impl(:lib_base, :lib_network)` |

- `settings.gradle.kts` 已注册 `:data` / `:designsystem`
- 全局替换：`com.example.sheeps.core.R` → `com.example.sheeps.ui.R`、`com.example.sheeps.theme` → `com.example.sheeps.ui.theme`
- 为消除 **data → core 环依赖**，将 `UserPreferences` / `NetworkMonitor` / `SkinConstants` 下沉到 `:data`

## 二、为什么这些类下沉到 :data
拆分后 `:data` 仍引用了原本在 `:core` 的 `UserPreferences`、`NetworkMonitor`、`SkinConstants`（`GameRepository`/`TokenRefresher`/`UserRepository`/`SyncRepository` 注入 `UserPreferences`；`SyncRepository` 用 `NetworkMonitor`；`UserPreferences` 用 `SkinConstants.DEFAULT_SKIN`）。若 `:data` 依赖 `:core` 会与 `:core api(:data)` 成环，故将它们归入数据层（本就是存储/网络/常量关注点）。

## 三、本地验证清单（必做）
```bash
# 1) 全量编译（最关键的门禁）
./gradlew :app:assembleDebug

# 2) Lint
./gradlew :app:lintDebug

# 3) 冒烟：splash → menu → game 跑通，登录/注册/商城/头像等 Toast 正常

# 4) 重生成 Baseline Profile（拆分模块后必须）
./gradlew :baselineprofile:generateBaselineProfile   # 需设备/模拟器
#   再将生成产物合回 release 基线

# 5) TheRouter 路由重生成（注解处理器会在 assemble 时自动重跑，确认 routeMap 正常）
```

## 四、已知风险 / 后续可优化
1. **`:data` 依赖 `:designsystem`**：因 `ErrorMessageMapper` 引用 `ui.R` 的 `toast_*` 字符串（data→UI 分层小异味）。更干净的做法是把这 5 个 `toast_*` 字符串移入 `:data` 的 `res/`，并让 `feature_menu` 改为引用 `com.example.sheeps.data.R.string.toast_*`。
2. **Hilt 跨模块**：`:core` 的 `@Module`（`NetworkModule`/`StorageModule`）提供 `:data` 的类（ApiService/UserPreferences），属正常跨模块 DI；构建时确认 Singleton 组件覆盖全工程。
3. **依赖收紧未尽**：瘦身 `:core` 仍保留了 `room`/`coil`/`xxpermissions` 等 `implementation` 依赖（为安全未删，避免本地编译意外失败）；验证通过后可按 `docs/struct-opt-review.md` §2.5 继续收紧。

## 五、回滚
```bash
git revert 547a6ce1          # 撤销整个 Phase 2+3（单提交）
# 或
git checkout 547a6ce1~1       # 回到拆分前
```
