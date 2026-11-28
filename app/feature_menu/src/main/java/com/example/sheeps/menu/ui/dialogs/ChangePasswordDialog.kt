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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sheeps.ui.R
import com.example.sheeps.menu.ui.screens.VerificationCodeButton

@Composable
fun ChangePasswordDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onChangePassword: (String, String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    val hasEmail = currentEmail.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_change_password), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasEmail) {
                    Text(
                        stringResource(R.string.bind_email_before_change_pwd),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(stringResource(R.string.current_account_prefix, currentEmail), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = code, onValueChange = { code = it; error = null }, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.hint_code)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            VerificationCodeButton(currentEmail)
                        }
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it; error = null }, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.hint_new_password)) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    OutlinedTextField(
                        value = confirmation, onValueChange = { confirmation = it; error = null }, modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.hint_confirm_password)) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Text(stringResource(error ?: R.string.pwd_hint),
                        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (hasEmail) {
                Button(onClick = {
                    error = when {
                        password.length !in 6..20 -> R.string.pwd_err_length
                        !password.any(Char::isLetter) || !password.any(Char::isDigit) -> R.string.pwd_err_no_alpha_digit
                        password != confirmation -> R.string.pwd_err_not_match
                        code.length != 6 -> R.string.pwd_err_code_length
                        else -> null
                    }
                    if (error == null) onChangePassword(code, password)
                }) { Text(stringResource(R.string.btn_confirm)) }
            }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}
