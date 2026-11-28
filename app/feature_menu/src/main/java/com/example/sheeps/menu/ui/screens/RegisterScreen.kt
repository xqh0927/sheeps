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
fun RegisterScreen(
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onRegister: (String, String, String) -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val pwdErrLength = stringResource(id = R.string.pwd_err_length)
    val pwdErrNoAlphaDigit = stringResource(id = R.string.pwd_err_no_alpha_digit)
    val pwdErrNotMatch = stringResource(id = R.string.pwd_err_not_match)
    val pwdErrCodeLength = stringResource(id = R.string.pwd_err_code_length)


    AccountForm(stringResource(R.string.register_title), onBack, isLoading) {
        AccountField(email, stringResource(R.string.label_email), { email = it; errorMsg = null }, KeyboardType.Email)
        VerificationRow(code, { code = it; errorMsg = null }, email)
        AccountField(password, stringResource(R.string.label_password), { password = it; errorMsg = null }, KeyboardType.Password)
        AccountField(confirmPassword, stringResource(R.string.hint_confirm_password),
            { confirmPassword = it; errorMsg = null }, KeyboardType.Password)
        Text(stringResource(R.string.pwd_hint), color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        AccountSubmit(stringResource(R.string.btn_register), !isLoading) {
            errorMsg = when {
                password.length !in 6..20 -> pwdErrLength
                !password.any { it.isDigit() } || !password.any { it.isLetter() } -> pwdErrNoAlphaDigit
                password != confirmPassword -> pwdErrNotMatch
                code.length != 6 -> pwdErrCodeLength
                else -> null
            }
            if (errorMsg == null) {
                onRegister(email.trim(), password, code.trim())
                onRegisterSuccess()
            }
        }
        TextButton(onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.hint_already_have_account)) }
    }
}
