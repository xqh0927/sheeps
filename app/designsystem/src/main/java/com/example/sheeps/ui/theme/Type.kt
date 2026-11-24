package com.example.sheeps.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FontTitle = FontFamily.SansSerif
val FontBody = FontFamily.SansSerif

private fun display(size: Int, line: Int) = TextStyle(
    fontFamily = FontTitle, fontWeight = FontWeight.Bold, fontSize = size.sp,
    lineHeight = line.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = "tnum"
)

private fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontBody, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp
)

val Typography = Typography(
    displayLarge = display(56, 60), displayMedium = display(44, 50), displaySmall = display(36, 42),
    headlineLarge = display(34, 41), headlineMedium = display(26, 32), headlineSmall = display(22, 28),
    titleLarge = text(21, 27, FontWeight.Bold), titleMedium = text(16, 22, FontWeight.SemiBold),
    titleSmall = text(14, 20, FontWeight.SemiBold),
    bodyLarge = text(16, 25), bodyMedium = text(15, 22), bodySmall = text(12, 18),
    labelLarge = text(14, 20, FontWeight.SemiBold), labelMedium = text(12, 17, FontWeight.Medium),
    labelSmall = text(11, 16, FontWeight.Medium)
)
