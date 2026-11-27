package com.example.sheeps.menu.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sheeps.menu.R
import com.example.sheeps.menu.state.PersonalLoadState

@Composable
internal fun PersonalSectionFeedback(
    status: PersonalLoadState?, isEmpty: Boolean, @StringRes emptyMessage: Int,
    onRetry: () -> Unit, onLogin: () -> Unit
) {
    when (status) {
        null, PersonalLoadState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
        PersonalLoadState.Ready -> if (isEmpty) {
            Text(stringResource(emptyMessage), Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PersonalLoadState.SignInRequired, PersonalLoadState.Failed -> {
            val needsLogin = status == PersonalLoadState.SignInRequired
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (needsLogin) R.string.personal_session_expired else R.string.personal_load_failed),
                    Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(if (needsLogin) onLogin else onRetry) {
                    Text(stringResource(if (needsLogin) R.string.personal_sign_in else R.string.personal_retry))
                }
            }
        }
    }
}
