package com.example.sheeps.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.example.sheeps.ui.theme.Gold_Primary
import com.example.sheeps.ui.theme.Gold_Dark

/**
 * 统一的二级界面顶部导航栏，高度为 44dp。
 * 包含：返回按钮、居中标题、可选操作按钮（如刷新、暂停等）
 *
 * @param title 标题文字
 * @param onBack 返回按钮点击回调
 * @param modifier 修饰符
 * @param showAction 是否显示右侧操作按钮
 * @param actionIcon 右侧操作按钮的图标，默认为刷新图标
 * @param onActionClick 右侧操作按钮的点击回调
 * @param containerColor 背景色，默认 surface(含透明度)
 */
@Composable
fun SheepsTopAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showAction: Boolean = false,
    actionIcon: ImageVector = Icons.Default.Refresh,
    onActionClick: () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
) {
    // 随主题动态选择最适配的前景内容颜色（文字和图标）
    // 墨夜金 (Gold_Primary) 和 清日春 (Gold_Dark) 两个主题下使用 secondary 金黄色以对齐国风/暗色设计
    // 其他高对比度主题下（如樱花粉、森林绿、星空蓝等）自动使用 primary 品牌色，确保文字清晰可读
    val contentColor = if (
        MaterialTheme.colorScheme.secondary == Gold_Primary ||
        MaterialTheme.colorScheme.secondary == Gold_Dark
    ) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 4.dp)
        ) {
            // 左侧返回按钮
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = contentColor
                )
            }

            // 中间居中标题（Serif 字体，随主题色变化）
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            // 右侧操作按钮
            if (showAction) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = "操作",
                        tint = contentColor
                    )
                }
            } else {
                // 右侧占位以保持中间标题完美居中
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}
