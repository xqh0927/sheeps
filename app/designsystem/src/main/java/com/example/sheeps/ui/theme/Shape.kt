package com.example.sheeps.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
// 秘境消消乐 · 设计系统 - 圆角规范
// 视觉设计系统形状规范 Tokens
// =============================================================================

// --- 圆角尺寸 Token ---
val ShapeRadius_XSmall  = 4.dp   // 徽章、标签
val ShapeRadius_Small   = 8.dp   // 小按钮、Chip
val ShapeRadius_Medium  = 12.dp  // 卡片、对话框内容
val ShapeRadius_Large   = 16.dp  // 主卡片、大型对话框
val ShapeRadius_XLarge  = 20.dp  // 底部导航、大型容器
val ShapeRadius_Full    = 50.dp  // 完全圆形（按钮/头像）

// --- Compose Shape 实例 ---
val ShapeXSmall  = RoundedCornerShape(ShapeRadius_XSmall)
val ShapeSmall   = RoundedCornerShape(ShapeRadius_Small)
val ShapeMedium  = RoundedCornerShape(ShapeRadius_Medium)
val ShapeLarge   = RoundedCornerShape(ShapeRadius_Large)
val ShapeXLarge  = RoundedCornerShape(ShapeRadius_XLarge)
val ShapeFull    = RoundedCornerShape(ShapeRadius_Full)

// --- 底部圆角（BottomSheet 等）---
val ShapeBottomSheet = RoundedCornerShape(
    topStart = ShapeRadius_XLarge,
    topEnd = ShapeRadius_XLarge,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

// --- Material3 Shapes 配置 ---
val SheepsShapes = Shapes(
    extraSmall = ShapeXSmall,
    small       = ShapeSmall,
    medium      = ShapeMedium,
    large       = ShapeLarge,
    extraLarge  = ShapeXLarge
)
