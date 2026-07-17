package com.example.sheeps.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// 秘境消消乐 · 设计系统 - 颜色规范
// 视觉设计系统色彩规范 Tokens
// =============================================================================

// --- 主题一：墨夜金（默认暗色主题）Background/Surface ---
val MoYe_Background       = Color(0xFF0D1117) // 墨黑底色
val MoYe_Surface          = Color(0xFF161B27) // 卡片表面
val MoYe_SurfaceVariant   = Color(0xFF1E2535) // 次级表面
val MoYe_SurfaceContainer = Color(0xFF252D3D) // 容器背景
val MoYe_Outline          = Color(0xFF3A4258) // 边框
val MoYe_OutlineVariant   = Color(0xFF2A3048) // 次级边框

// --- 主题二：清日春（浅色主题）---
val QingRi_Background       = Color(0xFFFBF9F6) // 宣纸白
val QingRi_Surface          = Color(0xFFFFFDF9) // 卡片表面
val QingRi_SurfaceVariant   = Color(0xFFF3EFEB) // 次级表面
val QingRi_SurfaceContainer = Color(0xFFEDE9E3) // 容器背景
val QingRi_Outline          = Color(0xFFD4C9BC) // 边框
val QingRi_OutlineVariant   = Color(0xFFE5DDD3) // 次级边框

// --- 主色：朱砂红 ---
val Crimson_Primary         = Color(0xFFC82423) // 朱砂正红
val Crimson_PrimaryDark     = Color(0xFF8B0000) // 暗朱
val Crimson_PrimaryLight    = Color(0xFFE85050) // 亮朱
val Crimson_PrimaryContainer= Color(0xFF4A0000) // 朱砂容器（暗色模式）
val Crimson_PrimaryContainerLight = Color(0xFFFFE8E8) // 朱砂容器（浅色模式）

// --- 金色系：帝王金 ---
val Gold_Primary            = Color(0xFFE6A23C) // 帝王金
val Gold_Light              = Color(0xFFF5C96B) // 亮金
val Gold_Dark               = Color(0xFFC49A2A) // 暗金
val Gold_Subtle             = Color(0xFFDFB76C) // 柔金（边框）
val Gold_Shimmer            = Color(0xFFFFF3CC) // 金光（shimmer高光）

// --- 功能色 ---
val Jade_Success            = Color(0xFF27AE60) // 翠玉绿（成功）
val Jade_SuccessLight       = Color(0xFF4CAF50) // 亮翠（+积分）
val Amber_Warning           = Color(0xFFFF8C42) // 琥珀橙（警告）
val Vermilion_Error         = Color(0xFFE53935) // 丹砂（错误）
val Sky_Info                = Color(0xFF4B9CE8) // 天青（信息）

// --- 文字色（暗色主题）---
val Text_Primary_Dark       = Color(0xFFF5E6C8) // 象牙白（主要文字）
val Text_Secondary_Dark     = Color(0xFFA89B7A) // 陈金（次要文字）
val Text_Disabled_Dark      = Color(0xFF5C6080) // 灰蓝（禁用文字）
val Text_OnPrimary          = Color(0xFFFFF8EE) // 朱砂上的文字

// --- 文字色（浅色主题）---
val Text_Primary_Light      = Color(0xFF1C1A17) // 深墨（主要文字）
val Text_Secondary_Light    = Color(0xFF6B6259) // 烟灰（次要文字）
val Text_Disabled_Light     = Color(0xFFBBB3AA) // 淡灰（禁用文字）

// --- 透明叠加层 ---
val Overlay_Dark_Heavy      = Color(0xCC000000) // 80% 黑遮罩
val Overlay_Dark_Medium     = Color(0x99000000) // 60% 黑遮罩
val Overlay_Dark_Light      = Color(0x33000000) // 20% 黑遮罩
val Overlay_Gold_Glow       = Color(0x33E6A23C) // 金色光晕
val Overlay_Crimson_Glow    = Color(0x22C82423) // 朱砂光晕

// --- 特殊 UI 颜色 ---
val Tray_Background_Dark    = Color(0xFF111522) // 置物架背景
val Slot_Border             = Color(0xFFB71C1C) // 消除槽边框
val Sealed_Talisman_Tint    = Color(0xB3E6A23C) // 封印符纸颜色（70%金）
val Leaderboard_Gold        = Color(0xFFFFD700) // 金牌
val Leaderboard_Silver      = Color(0xFFC0C0C0) // 银牌
val Leaderboard_Bronze      = Color(0xFFCD7F32) // 铜牌

// =============================================================================
// 四套女性向主题色版：樱花粉 / 星空蓝 / 暖阳橙 / 森林绿
// =============================================================================

// --- 樱花粉 (Sakura) ---
val Sakura_Primary          = Color(0xFFEC407A)
val Sakura_PrimaryLight     = Color(0xFFF06292)
val Sakura_PrimaryDark      = Color(0xFFC2185B)
val Sakura_Background       = Color(0xFFFFF5F8)
val Sakura_Surface          = Color(0xFFFFFAFC)
val Sakura_SurfaceVariant   = Color(0xFFFFF0F5)
val Sakura_SurfaceContainer = Color(0xFFFCE4EC)
val Sakura_Outline          = Color(0xFFF8BBD0)
val Sakura_OutlineVariant   = Color(0xFFFCE4EC)
val Sakura_Gold_Subtle      = Color(0xFFFF4081)

// --- 星空蓝 (Cosmic) ---
val Cosmic_Primary          = Color(0xFF3F51B5)
val Cosmic_PrimaryLight     = Color(0xFF5C6BC0)
val Cosmic_PrimaryDark      = Color(0xFF303F9F)
val Cosmic_Background       = Color(0xFFEEF0FA)
val Cosmic_Surface          = Color(0xFFF8F9FD)
val Cosmic_SurfaceVariant   = Color(0xFFE8EAF6)
val Cosmic_SurfaceContainer = Color(0xFFE8EAF6)
val Cosmic_Outline          = Color(0xFFC5CAE9)
val Cosmic_OutlineVariant   = Color(0xFFE8EAF6)
val Cosmic_Gold_Subtle      = Color(0xFF7986CB)

// --- 暖阳橙 (Sunset) ---
val Sunset_Primary          = Color(0xFFFF9800)
val Sunset_PrimaryLight     = Color(0xFFFFB74D)
val Sunset_PrimaryDark      = Color(0xFFE65100)
val Sunset_Background       = Color(0xFFFFF8E1)
val Sunset_Surface          = Color(0xFFFFFDF5)
val Sunset_SurfaceVariant   = Color(0xFFFFF3E0)
val Sunset_SurfaceContainer = Color(0xFFFFF3E0)
val Sunset_Outline          = Color(0xFFFFE0B2)
val Sunset_OutlineVariant   = Color(0xFFFFF3E0)
val Sunset_Gold_Subtle      = Color(0xFFFFB74D)

// --- 森林绿 (Forest) ---
val Forest_Primary          = Color(0xFF4CAF50)
val Forest_PrimaryLight     = Color(0xFF81C784)
val Forest_PrimaryDark      = Color(0xFF2E7D32)
val Forest_Background       = Color(0xFFF1F8E9)
val Forest_Surface          = Color(0xFFF8FBF5)
val Forest_SurfaceVariant   = Color(0xFFE8F5E9)
val Forest_SurfaceContainer = Color(0xFFE8F5E9)
val Forest_Outline          = Color(0xFFC8E6C9)
val Forest_OutlineVariant   = Color(0xFFE8F5E9)
val Forest_Gold_Subtle      = Color(0xFF81C784)

// --- 萌趣竞技 (ShuangFun / 爽爽蓝) ---
val ShuangFun_Primary          = Color(0xFF29B6F6)  // 爽爽蓝（主色）
val ShuangFun_PrimaryLight     = Color(0xFF4FC3F7)  // 亮蓝
val ShuangFun_PrimaryDark      = Color(0xFF0288D1)  // 深蓝
val ShuangFun_Background       = Color(0xFFE1F5FE)  // 乐园浅蓝背景
val ShuangFun_Surface          = Color(0xFFFFFFFF)  // 奶油白卡片
val ShuangFun_SurfaceVariant   = Color(0xFFF0F9FF)  // 次级表面
val ShuangFun_SurfaceContainer = Color(0xFFE3F2FD)  // 容器背景
val ShuangFun_Outline          = Color(0xFF81D4FA)  // 边框色
val ShuangFun_OutlineVariant   = Color(0xFFB3E5FC)  // 次级边框
val ShuangFun_Gold_Subtle      = Color(0xFFFFCA28)  // 阳光金（装饰/奖励）

// =============================================================================
// 向后兼容别名（保留旧名称引用，避免大规模重命名编译错误）
// =============================================================================
val CrimsonRed      = Crimson_Primary
val ImperialYellow  = Gold_Primary
val JadeGreen       = Jade_Success
val CharcoalDark    = MoYe_Background
val GoldenBronze    = Gold_Subtle
val PaperWhite      = QingRi_Background
val SoftGrey        = QingRi_SurfaceVariant
val TextDark        = Text_Primary_Light
val TextLight       = Text_Primary_Dark
