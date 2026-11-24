package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.sheeps.ui.R

@Composable
fun NicknameEditDialog(currentNickname: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var nickname by rememberSaveable(currentNickname) { mutableStateOf(currentNickname) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_change_nickname), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(value = nickname, onValueChange = { nickname = it },
                label = { Text(stringResource(R.string.hint_nickname)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { Button(onClick = { onSave(nickname.trim()) }, enabled = nickname.isNotBlank()) { Text(stringResource(R.string.btn_save)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}
