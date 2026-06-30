# 道具携带限制与置灰显示设计方案

## 需求背景
目前游戏在备战阶段允许玩家选择并携带任意数量的道具（最多可达8种），这在进入游戏关卡后，底部的“已携带法宝”面板会因为道具过多导致排版拥挤并发生重叠/换行截断的问题。
为了提升游戏平衡性及界面的美观性：
1. **限制携带道具种类数量**：玩家每局游戏最多只能选择 5 种道具进行闯关。
2. **未选中道具置灰显示**：
   - 在**关卡备战弹窗**（`PrepareGameDialog`）中，未选中（数量为 0）的道具图标置灰。
   - 在**游戏关卡内**（`GameScreen`）下方的“已携带法宝”区域，始终展示 5 个槽位。若玩家携带不足 5 种道具，空余的槽位显示为置灰的“未选择”虚线框/通用占位图。

---

## 详细设计

### 1. 道具图标置灰能力支持
在核心 UI 组件 `ItemAnimationIcon` 中增加 `isGray: Boolean` 参数。
- 若 `isGray = true`，使用 Compose 的 `ColorFilter` 与 `graphicsLayer` 进行置灰渲染：
  ```kotlin
  Modifier.drawWithContent {
      val paint = Paint().apply {
          colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
      }
      drawIntoCanvas { canvas ->
          canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
          drawContent()
          canvas.restore()
      }
  }.graphicsLayer(alpha = 0.5f)
  ```

### 2. 备战弹窗（`PrepareGameDialog`）限制与置灰
- **限制最多选择 5 种道具**：
  在 Compose 状态中计算已选道具种类数量：
  `val selectedTypesCount = state.selectedCarryItems.count { it.value > 0 }`
  当 `selectedTypesCount >= 5` 且当前道具未选择（`selected == 0`）时，**禁用当前道具的增加（“+”）按钮**，防止玩家选择第 6 种道具。
- **未选中置灰**：
  将 `isGray = (selected == 0)` 传入 `ItemAnimationIcon`，使用户一眼能看出哪些道具已被选择放入备战包。

### 3. 数据层拦截与安全控制（`MenuViewModel`）
在 `handleUpdateCarryItem` 逻辑中进行二次拦截，确保数据层面的安全性，当尝试选择第 6 种不同道具时，通过 Toast 提示用户，并拒绝增加操作。

### 4. 游戏内携带栏（`GameScreen`）占位展示
- “已携带法宝”区域的展示逻辑重构为固定展示 5 个槽位。
- 提取已选择携带的道具列表（数量 > 0）。
- 若携带的道具种类数不足 5，剩余的槽位渲染为“未选择”状态：
  - 显示为带有圆角虚线/细描边的灰色框。
  - 内部放置“空”或加号图标，底部文字显示为“未选择”且呈置灰样式。

---

## 影响文件清单
- **[MODIFY] [ItemAnimationIcon.kt](file:///e:/file/sheeps/app/core/src/main/java/com/example/sheeps/ui/components/ItemAnimationIcon.kt)**: 增加 `isGray` 属性及置灰特效。
- **[MODIFY] [PrepareGameDialog.kt](file:///e:/file/sheeps/app/feature_menu/src/main/java/com/example/sheeps/menu/ui/dialogs/PrepareGameDialog.kt)**: 增加携带道具数 5 种的上限校验、置灰未选图标。
- **[MODIFY] [MenuViewModel.kt](file:///e:/file/sheeps/app/feature_menu/src/main/java/com/example/sheeps/menu/viewmodel/MenuViewModel.kt)**: 在 `handleUpdateCarryItem` 中加入 5 种道具上限拦截与 Toast 提示。
- **[MODIFY] [GameScreen.kt](file:///e:/file/sheeps/app/feature_game/src/main/java/com/example/sheeps/game/ui/screens/GameScreen.kt)**: 重构已携带法宝展示逻辑，支持固定 5 槽位及空槽位置灰占位展示。

---

## 验证计划
1. **备战限制测试**：
   - 打开关卡备战弹窗，选择 5 种不同的道具。
   - 确认第 6 种道具的“+”按钮变为禁用状态。
   - 减少其中一种道具到 0 后，确认其他原本禁用的“+”按钮恢复为可用状态。
2. **置灰效果测试**：
   - 确认备战弹窗中数量为 0 的道具图标均呈现置灰、半透明样式。
   - 确认增加数量后，图标立刻恢复彩色状态。
3. **关卡携带栏测试**：
   - 携带 3 个道具进入关卡。
   - 确认下方“已携带法宝”显示 3 个彩色的道具图标，以及 2 个“未选择”的置灰占位框。
   - 确认没有任何文字截断或重叠现象。
