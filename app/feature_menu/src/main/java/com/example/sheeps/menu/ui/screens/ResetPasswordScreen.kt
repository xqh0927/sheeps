package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.sheeps.ui.R

@Composable
fun ResetPasswordScreen(
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onReset: (String, String, String) -> Unit,
    onResetSuccess: () -> Unit
) {
    var phone by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // 预先在 composable 作用域内捕获本地化文案，避免在 onClick（非 composable lambda）中调用 stringResource
    val pwdErrLength = stringResource(R.string.pwd_err_length)
    val pwdErrAlphanumeric = stringResource(R.string.pwd_err_alphanumeric)
    val pwdErrCodeLength = stringResource(R.string.pwd_err_code_length)


    AccountForm(stringResource(R.string.reset_password_title), onBack, isLoading) {
        AccountField(phone, stringResource(R.string.label_phone), { phone = it; errorMsg = null }, KeyboardType.Phone)
        VerificationRow(code, { code = it; errorMsg = null }, phone)
        AccountField(newPassword, stringResource(R.string.label_new_password), { newPassword = it; errorMsg = null }, KeyboardType.Password)
        Text(stringResource(R.string.pwd_hint), color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        AccountSubmit(stringResource(R.string.btn_confirm), !isLoading) {
            errorMsg = when {
                newPassword.length !in 6..20 -> pwdErrLength
                !newPassword.any { it.isDigit() } || !newPassword.any { it.isLetter() } -> pwdErrAlphanumeric
                code.length != 6 -> pwdErrCodeLength
                else -> null
            }
            if (errorMsg == null) {
                onReset(phone, code, newPassword)
                onResetSuccess()
            }
        }
    }
}
