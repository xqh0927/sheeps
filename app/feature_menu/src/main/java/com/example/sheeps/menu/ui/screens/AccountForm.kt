package com.example.sheeps.menu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sheeps.ui.R
import com.example.sheeps.ui.components.FullScreenLoading

@Composable
internal fun AccountForm(
    title: String,
    onBack: () -> Unit,
    isLoading: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(144.dp).clip(RoundedCornerShape(26.dp))) {
                    Image(painterResource(R.drawable.sheep_glass_art), null,
                        modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop,
                        alignment = Alignment.CenterEnd)
                    IconButton(onClick = onBack, modifier = Modifier.padding(6.dp).size(46.dp)
                        .background(Color.White.copy(alpha = .78f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.btn_back),
                            tint = Color(0xFF232333), modifier = Modifier.size(30.dp))
                    }
                }
                Text(title, modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp),
                    style = MaterialTheme.typography.headlineLarge, color = colors.onBackground)
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            }
        }
        if (isLoading) FullScreenLoading()
    }
}

@Composable
internal fun AccountField(
    value: String, label: String, onChange: (String) -> Unit,
    type: KeyboardType, modifier: Modifier = Modifier
) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        visualTransformation = if (type == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ))
}

@Composable
internal fun VerificationRow(value: String, onChange: (String) -> Unit, countdown: Int, onRequest: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        AccountField(value, stringResource(R.string.label_code), onChange, KeyboardType.Number, Modifier.weight(1f))
        OutlinedButton(onRequest, enabled = countdown == 0, shape = CircleShape) {
            Text(if (countdown > 0) "${countdown}s" else stringResource(R.string.btn_get_code))
        }
    }
}

@Composable
internal fun AccountSubmit(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        shape = CircleShape) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
    }
}
