package com.example.sheeps.menu.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.sheeps.data.model.DeletionRequestStatus
import com.example.sheeps.data.repository.UserRepository
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.menu.R
import kotlinx.coroutines.launch

@Composable
fun AccountDeletionDialog(token: String, users: UserRepository, onDismiss: () -> Unit, onLogin: () -> Unit) {
    val scope = rememberCoroutineScope()
    var status by remember(token) { mutableStateOf<ApiResult<DeletionRequestStatus>?>(null) }
    var busy by remember(token) { mutableStateOf(true) }

    LaunchedEffect(token) {
        status = users.deletionStatus(token)
        busy = false
    }
    val error = status as? ApiResult.Error
    val pending = (status as? ApiResult.Success)?.data?.requested_at != null
    val action = when {
        error?.code == 401 -> R.string.personal_sign_in
        error != null -> R.string.personal_retry
        pending -> R.string.deletion_withdraw
        else -> R.string.deletion_submit
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_security)) },
        text = {
            if (busy) LinearProgressIndicator()
            else Text(stringResource(when {
                error?.code == 401 -> R.string.personal_session_expired
                error != null -> R.string.personal_load_failed
                pending -> R.string.deletion_pending
                else -> R.string.deletion_explain
            }))
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                if (error?.code == 401) {
                    onLogin()
                } else {
                    busy = true
                    scope.launch {
                        status = when {
                            error != null -> users.deletionStatus(token)
                            pending -> users.withdrawDeletion(token)
                            else -> users.requestDeletion(token)
                        }
                        busy = false
                    }
                }
            }) { Text(stringResource(action)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}
