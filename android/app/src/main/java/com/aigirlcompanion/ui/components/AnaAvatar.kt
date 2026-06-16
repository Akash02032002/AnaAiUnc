package com.aigirlcompanion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigirlcompanion.R

@Composable
fun AnaAvatar(
    emotion: String,
    modifier: Modifier = Modifier,
    showName: Boolean = true,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                        )
                    )
                )
                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ana_avatar),
                contentDescription = "Ana avatar",
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop,
            )
        }
        if (showName) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ANA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = emotionColor(emotion).copy(alpha = 0.14f),
            ) {
                Text(
                    text = emotion.replaceFirstChar { it.uppercase() },
                    modifier = Modifier
                        .background(Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    color = emotionColor(emotion),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun emotionColor(emotion: String): Color {
    return when (emotion.lowercase()) {
        "happy" -> Color(0xFF2EAD63)
        "sad" -> Color(0xFF5B7CFA)
        "loving" -> Color(0xFFFF4F87)
        "playful" -> Color(0xFFFF8A3D)
        "angry" -> Color(0xFFE53935)
        "caring" -> Color(0xFF3D7DFF)
        "serious" -> Color(0xFF6B7280)
        else -> MaterialTheme.colorScheme.primary
    }
}
