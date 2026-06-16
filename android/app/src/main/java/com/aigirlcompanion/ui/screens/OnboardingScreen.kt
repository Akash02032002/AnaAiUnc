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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.aigirlcompanion.data.RelationshipStyles
import com.aigirlcompanion.data.UserProfile
import com.aigirlcompanion.ui.components.AnaAvatar
import com.aigirlcompanion.ui.components.RelationshipStylePicker

@Composable
fun OnboardingScreen(
    profile: UserProfile,
    onComplete: (UserProfile) -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    var birthday by remember { mutableStateOf(profile.birthday) }
    var relationshipStyle by remember { mutableStateOf(profile.relationshipStyle) }
    var confirmed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnaAvatar(emotion = "loving")
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "AI Girl Companion",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Meet Ana, your adult 18+ AI companion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your name") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = birthday,
                onValueChange = { birthday = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Birthday") },
                placeholder = { Text("Example: 15 August") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(20.dp))
            RelationshipStylePicker(
                title = "Relationship style",
                styles = RelationshipStyles,
                selected = relationshipStyle,
                onSelected = { relationshipStyle = it },
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = confirmed,
                    onCheckedChange = { confirmed = it },
                )
                Text(
                    text = "I confirm that I am at least 18 years old and will use this app responsibly.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (error.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    error = when {
                        name.isBlank() -> "Please enter your name."
                        !confirmed -> "You must confirm that you are 18+ to continue."
                        else -> ""
                    }
                    if (error.isBlank()) {
                        onComplete(
                            profile.copy(
                                name = name.trim(),
                                birthday = birthday.trim(),
                                relationshipStyle = relationshipStyle,
                                adultConfirmed = true,
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start")
            }
        }
    }
}

