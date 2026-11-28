package com.example.sheeps.menu.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sheeps.menu.ui.screens.VerificationCodeButton
import com.example.sheeps.menu.viewmodel.isValidEmailInput
import com.example.sheeps.ui.R

@Composable
fun BindEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onBindEmail: (String, String) -> Unit
) {
    var email by remember { mutableStateOf(currentEmail) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_bind_email), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.hint_code)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { VerificationCodeButton(email) }
                )
                error?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                error = when {
                    !isValidEmailInput(email) -> R.string.err_invalid_email
                    code.trim().length != 6 -> R.string.pwd_err_code_length
                    else -> null
                }
                if (error == null) onBindEmail(email.trim(), code.trim())
            }) { Text(stringResource(R.string.btn_confirm)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}

@Composable
fun BindPhoneDialog(
    currentPhone: String,
    onDismiss: () -> Unit,
    onBindPhone: (String) -> Unit
) {
    var phone by remember { mutableStateOf(currentPhone) }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_bind_phone), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Text(
                    stringResource(error ?: R.string.hint_bind_phone),
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val trimmed = phone.trim()
                error = if (trimmed.length != 11 || !trimmed.startsWith('1') || trimmed.any { it !in '0'..'9' }) {
                    R.string.err_invalid_phone
                } else null
                if (error == null) onBindPhone(trimmed)
            }) { Text(stringResource(R.string.btn_confirm)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}
