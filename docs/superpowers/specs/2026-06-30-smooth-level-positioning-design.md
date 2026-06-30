# 2026-06-30 关卡列表平滑定位与冷启动限制设计方案

该设计文档旨在解决关卡列表的自动定位/手动定位动画不流畅的问题，并将自动定位限制为仅在冷启动 App 时生效。

## 目标与修改要求

- **冷启动判定**：自动定位逻辑仅在应用程序启动（冷启动）后的第一次进入主界面有效，后续热启动返回该界面时不再自动触发滚动。
- **平滑滚动动画**：
  - 自动定位由直接跳转改为平滑的滚动动画。
  - 为防止长距离滚动时需要实时渲染大量中间卡片而引发的 Compose 掉帧卡顿，设计一个 `LazyListState.animateScrollToItemSmoothly()` 扩展函数。
  - 当滚动跨度大于 4 个项时，先闪现（`scrollToItem`）至目标项附近（相差 3 个项），再进行平滑动画滚动到最终目标，消除长距离滚动卡顿。

## 涉及文件与修改点

### 1. [MODIFY] [GameHomeScreen.kt](file:///c:/Users/15613/Documents/file/sheeps/app/feature_menu/src/main/java/com/example/sheeps/menu/ui/screens/GameHomeScreen.kt)

- 在文件顶部或底部定义一个文件级私有变量：
  ```kotlin
  private var isColdStartAutoScrolled = false
  ```
- 定义 `LazyListState.animateScrollToItemSmoothly` 扩展函数：
  ```kotlin
  private suspend fun LazyListState.animateScrollToItemSmoothly(index: Int, scrollOffset: Int = 0) {
      val visibleItems = layoutInfo.visibleItemsInfo
      if (visibleItems.isEmpty()) {
          scrollToItem(index, scrollOffset)
          return
      }
      val firstVisible = visibleItems.first().index
      val lastVisible = visibleItems.last().index
      if (index in firstVisible..lastVisible) {
          animateScrollToItem(index, scrollOffset)
          return
      }
      val distance = index - firstVisible
      if (kotlin.math.abs(distance) > 4) {
          val snapIndex = if (distance > 0) index - 3 else index + 3
          scrollToItem(snapIndex.coerceIn(0, layoutInfo.totalItemsCount - 1))
      }
      animateScrollToItem(index, scrollOffset)
  }
  ```
- 将 `GameHomeScreen` 内的 `hasAutoScrolled` 初始化更改为以 `isColdStartAutoScrolled` 为初始状态，并在自动滚动完成后更新 `isColdStartAutoScrolled` 和 `hasAutoScrolled`。
- 修改自动定位的 `LaunchedEffect`，增加至 `300ms` 延迟并调用 `animateScrollToItemSmoothly`。
- 修改手动定位按钮的点击事件，调用 `animateScrollToItemSmoothly` 代替原有的 `animateScrollToItem`。

## 验证计划

- 确认编译通过。
- 启动应用（冷启动），验证关卡列表是否展示短暂延时后平滑滚动到当前解锁的最新关卡。
- 进入其他页面（如关卡或排行榜）后再返回主页（热启动），验证关卡列表没有再次发生滚动。
- 手动滑动远离当前解锁关卡，点击定位按钮，验证平滑无卡顿回滚。
