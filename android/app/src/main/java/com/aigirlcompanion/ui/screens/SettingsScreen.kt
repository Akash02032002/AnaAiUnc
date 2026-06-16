package com.aigirlcompanion.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigirlcompanion.data.UserProfile

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    onDeleteAll: () -> Unit,
) {
    var backendUrl by remember(profile.backendUrl) { mutableStateOf(profile.backendUrl) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Safety, backend, theme, and local data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
            Spacer(modifier = Modifier.height(18.dp))
            SettingPanel(title = "Appearance") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Dark mode")
                    Switch(
                        checked = profile.darkTheme,
                        onCheckedChange = { onProfileChange(profile.copy(darkTheme = it)) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingPanel(title = "Backend") {
                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Backend URL") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onProfileChange(profile.copy(backendUrl = backendUrl.trim())) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save Backend URL")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingPanel(title = "Safety Rules") {
                Text(
                    text = "Romance and flirting are allowed. Explicit sexual content, minor-related sexual content, illegal instructions, exploitation, and harmful requests are blocked. Unsafe chats may pause Ana temporarily.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingPanel(title = "Local Data") {
                Text(
                    text = "This MVP stores profile, chat, and memory on this device. Live AI messages are sent to your backend when it is reachable.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDeleteAll,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete All Local Data")
                }
            }
        }
    }
}

@Composable
private fun SettingPanel(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

