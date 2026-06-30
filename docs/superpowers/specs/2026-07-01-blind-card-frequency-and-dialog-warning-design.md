# 2026-07-01 盲盒牌出现频率调整与进入游戏对话框难度警示设计方案

该设计文档旨在优化盲盒卡牌（不可见图案卡牌）的出现逻辑及其对应的界面提示：
1. **出现频率调整**：将原来写死在 `levelId % 10 == 0`（仅每逢10关）的盲盒卡牌生成逻辑，修改为从第 3 关起每 3 关出现一次（即 `levelId >= 3 && levelId % 3 == 0`）。
2. **三端逻辑对齐**：同步修改本地关卡生成器、服务端生成器，确保各处的关卡规则高度契合。
3. **游戏前置警示弹窗**：在进入游戏前的准备弹窗（`PrepareGameDialog`）中，当检测到目标关卡包含盲盒卡牌时，动态展示醒目的橙红色警示 Banner，提示玩家“本关已启用「盲盒牌」，难度大幅提升！”。

## 涉及文件与修改点

### 1. [MODIFY] [GameLevelGenerator.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/viewmodel/helpers/GameLevelGenerator.kt)

- 修改 `generateSolvableLevelLocal` 中的盲盒牌生成判定：
  - 将：
    ```kotlin
    if (levelId >= 2) {
        val r = randProps()
        if (levelId % 10 == 0) {
            if (r < 0.15) {
                isBlind = true
            } else if (r < 0.30) {
                sealed = 1
            }
        }
    }
    ```
    修改为：
    ```kotlin
    if (levelId >= 3) {
        val r = randProps()
        if (levelId % 3 == 0) {
            if (r < 0.15) {
                isBlind = true
            }
        }
        if (!isBlind && r < 0.30) {
            sealed = 1
        }
    } else if (levelId >= 2) {
        val r = randProps()
        if (r < 0.30) {
            sealed = 1
        }
    }
    ```

### 2. [MODIFY] [level.ts](file:///c:/Users/15613/Documents/file/sheeps/server/src/level.ts)

- 服务端关卡生成的属性分配同步做相同规则修改，保证在 `levelId >= 3 && levelId % 3 === 0` 且概率触发时才产生盲盒卡牌。

### 3. [MODIFY] [PrepareGameDialog.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_menu/src/main/java/com/example/sheeps/menu/ui/dialogs/PrepareGameDialog.kt)

- 在未锁定关卡选择界面渲染时，如果 `levelId >= 3 && levelId % 3 == 0`，则在头部文字下方绘制一个设计精美的橙红色警示卡片组件，输出难度提升提醒文案。

## 验证计划

- 确认编译通过。
- 点击第 2 关，在准备弹窗中确认**不显示**难度提升提示。
- 点击第 3 关，在准备弹窗中确认**显示**“⚠️ 秘境深处迷雾重重！本关已启用「盲盒牌」，卡牌下方图案不可见，难度大幅提升！”的警示卡片。
- 进入第 3 关，确认关卡中确实生成了背面向上的盲盒卡牌；进入第 4 关，确认没有盲盒卡牌。
