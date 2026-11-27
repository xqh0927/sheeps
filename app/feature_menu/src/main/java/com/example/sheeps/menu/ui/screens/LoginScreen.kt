package com.example.sheeps.menu.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.sheeps.lib_network.AppConfig
import com.example.sheeps.ui.R
import com.hjq.toast.Toaster

@Composable
fun LoginScreen(
    isLoading: Boolean = false, onBack: () -> Unit,
    onLogin: (String, String) -> Unit, onPasswordLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit, onNavigateToResetPassword: () -> Unit
) {
    val context = LocalContext.current
    var passwordMode by rememberSaveable { mutableStateOf(false) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(false) }

    AccountForm(stringResource(R.string.btn_login), onBack, isLoading) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp).selectableGroup()) {
            listOf(R.string.tab_login_code, R.string.tab_login_password).forEachIndexed { index, id ->
                val active = (index == 1) == passwordMode
                val shape = RoundedCornerShape(9.dp)
                Box(Modifier.weight(1f).clip(shape)
                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .selectable(active, role = Role.Tab) { passwordMode = index == 1 }
                    .heightIn(min = 42.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(id), style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        AccountField(phone, stringResource(R.string.label_phone), { phone = it }, KeyboardType.Phone)
        AnimatedContent(passwordMode, transitionSpec = {
            (slideInVertically(tween(210)) { it / 4 } + fadeIn(tween(180))) togetherWith fadeOut(tween(100))
        }, label = "credentialField") { enterPassword ->
            if (enterPassword) {
                AccountField(password, stringResource(R.string.label_password), { password = it }, KeyboardType.Password)
            } else {
                VerificationRow(code, { code = it }, phone)
            }
        }
        LoginConsent(agreed) { agreed = it }
        AccountSubmit(stringResource(R.string.dialog_login_btn), agreed && !isLoading) {
            if (passwordMode) onPasswordLogin(phone, password) else onLogin(phone, code)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onNavigateToRegister) { Text(stringResource(R.string.register_title)) }
            if (passwordMode) TextButton(onClick = onNavigateToResetPassword) {
                Text(stringResource(R.string.login_forgot_password))
            }
        }
    }
}

@Composable
private fun LoginConsent(agreed: Boolean, onAgree: (Boolean) -> Unit) {
    val uri = LocalUriHandler.current
    val line = stringResource(R.string.login_agreement_hint)
    val color = MaterialTheme.colorScheme.primary
    val annotated = buildAnnotatedString {
        append(line)
        listOf(stringResource(R.string.auth_terms_link) to "agreement.html",
            stringResource(R.string.auth_privacy_link) to "privacy.html").forEach { (label, path) ->
            val start = line.indexOf(label)
            if (start >= 0) {
                addStyle(SpanStyle(color = color, textDecoration = TextDecoration.Underline), start, start + label.length)
                addStringAnnotation("URL", AppConfig.BASE_URL + path, start, start + label.length)
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = agreed, onCheckedChange = onAgree)
        ClickableText(annotated, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { uri.openUri(it.item) }
        }
    }
}
