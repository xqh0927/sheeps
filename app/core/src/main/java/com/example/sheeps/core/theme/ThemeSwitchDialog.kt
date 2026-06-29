package com.example.sheeps.core.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ThemeSwitchDialog(
    onDismiss: () -> Unit
) {
    val currentMode by ThemeManager.themeState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "选择主题模式")
        },
        text = {
            Column {
                ThemeMode.values().forEach { mode ->
                    val displayName = when (mode) {
                        ThemeMode.FOLLOW_SYSTEM -> "跟随系统"
                        ThemeMode.LIGHT -> "日间亮色"
                        ThemeMode.DARK -> "夜间暗色"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeManager.themeMode = mode
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = displayName)
                        RadioButton(
                            selected = (currentMode == mode),
                            onClick = {
                                ThemeManager.themeMode = mode
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
