package com.aigirlcompanion.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.widget.Toast
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigirlcompanion.R
import com.aigirlcompanion.data.ChatMessage
import com.aigirlcompanion.data.ChatRepository
import com.aigirlcompanion.data.UserProfile
import com.aigirlcompanion.data.cleanTextForSpeech
import com.aigirlcompanion.data.relationshipLabel
import com.aigirlcompanion.ui.components.emotionColor
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    messages: List<ChatMessage>,
    sending: Boolean,
    lastEmotion: String,
    onSend: (String, (String) -> Unit) -> Unit,
    onSendDocument: (String, ByteArray, String, (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onDeleteMessage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ChatRepository() }

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var isPlayingVoice by remember { mutableStateOf(false) }
    var isVoicePending by remember { mutableStateOf(false) }
    var playingMessageId by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        TextToSpeech(context) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
        }
    }

    LaunchedEffect(ttsReady) {
        if (ttsReady) {
            configureSweetGirlVoice(tts)
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isPlayingVoice = true
                }
                override fun onDone(utteranceId: String?) {
                    isPlayingVoice = false
                    playingMessageId = null
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isPlayingVoice = false
                    playingMessageId = null
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    isPlayingVoice = false
                    playingMessageId = null
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                player?.stop()
                player?.release()
            } catch (_: Exception) {}
            try {
                tts.stop()
                tts.shutdown()
            } catch (_: Exception) {}
        }
    }

    fun stopVoice() {
        isVoicePending = false
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}
        player = null
        try {
            tts.stop()
        } catch (_: Exception) {}
        isPlayingVoice = false
        playingMessageId = null
    }

    fun speakReply(reply: String, messageId: String? = null) {
        val cleanSpeechText = cleanTextForSpeech(reply)
        stopVoice()
        isPlayingVoice = true
        playingMessageId = messageId
        scope.launch {
            try {
                val audioFile = File(context.cacheDir, "ana_chat_voice_reply.mp3")
                val hasNeuralVoice = repository.synthesizeVoice(profile, cleanSpeechText, audioFile)
                if (hasNeuralVoice && isPlayingVoice) {
                    player = MediaPlayer().apply {
                        setDataSource(audioFile.absolutePath)
                        setOnCompletionListener {
                            it.release()
                            if (player == it) {
                                player = null
                                isPlayingVoice = false
                                playingMessageId = null
                            }
                        }
                        setOnErrorListener { mediaPlayer, _, _ ->
                            mediaPlayer.release()
                            if (player == mediaPlayer) {
                                player = null
                            }
                            if (ttsReady && isPlayingVoice) {
                                tts.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, null, "ana_reply")
                            } else {
                                isPlayingVoice = false
                                playingMessageId = null
                            }
                            true
                        }
                        prepare()
                        start()
                    }
                } else if (isPlayingVoice) {
                    if (ttsReady) {
                        tts.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, null, "ana_reply")
                    } else {
                        isPlayingVoice = false
                        playingMessageId = null
                    }
                }
            } catch (e: Exception) {
                if (ttsReady && isPlayingVoice) {
                    tts.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, null, "ana_reply")
                } else {
                    isPlayingVoice = false
                    playingMessageId = null
                }
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                isVoicePending = true
                onSend(spoken) { reply ->
                    if (isVoicePending) {
                        speakReply(reply)
                    }
                }
            } else {
                isVoicePending = false
            }
        } else {
            isVoicePending = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Ana")
                }
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Voice recognition unavailable on this device", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is needed for voice", Toast.LENGTH_SHORT).show()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName: String? = null
                if (uri.scheme == "content") {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx != -1) fileName = it.getString(idx)
                        }
                    }
                }
                val resolvedName = fileName ?: (uri.lastPathSegment ?: "document.pdf")
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    val promptText = input.trim()
                    input = ""
                    onSendDocument(resolvedName, bytes, promptText) { reply ->
                        speakReply(reply)
                    }
                } else {
                    Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    val voiceActive = isPlayingVoice || isVoicePending

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ChatHeader(profile = profile, emotion = lastEmotion)
        if (sending) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            if (messages.isEmpty()) {
                item {
                    EmptyChat(profile = profile)
                }
            }
            items(messages, key = { it.id }) { message ->
                val isThisMessagePlaying = isPlayingVoice && playingMessageId == message.id
                ChatBubble(
                    message = message,
                    isPlaying = isThisMessagePlaying,
                    onTogglePlay = if (message.role == "assistant") {
                        {
                            if (isThisMessagePlaying) {
                                stopVoice()
                            } else {
                                speakReply(message.content, message.id)
                            }
                        }
                    } else null,
                    onDelete = {
                        if (isThisMessagePlaying) {
                            stopVoice()
                        }
                        onDeleteMessage(message.id)
                    },
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        if (isPlayingVoice) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_voice),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Ana is speaking...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "Click Stop to mute",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Attach Document Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                IconButton(
                    onClick = { documentPickerLauncher.launch("*/*") },
                    enabled = !sending,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_attach),
                        contentDescription = "Attach Document",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message Ana") },
                minLines = 1,
                maxLines = 4,
            )

            // Small Voice Toggle Button (1st click: start voice / 2nd click: stop voice)
            Surface(
                shape = CircleShape,
                color = if (voiceActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
            ) {
                IconButton(
                    onClick = {
                        if (voiceActive) {
                            stopVoice()
                        } else {
                            val clean = input.trim()
                            if (clean.isNotBlank()) {
                                input = ""
                                isVoicePending = true
                                onSend(clean) { reply ->
                                    if (isVoicePending) {
                                        speakReply(reply)
                                    }
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    enabled = !sending || voiceActive,
                ) {
                    Icon(
                        painter = painterResource(id = if (voiceActive) R.drawable.ic_stop else R.drawable.ic_mic),
                        contentDescription = if (voiceActive) "Stop voice" else "Voice mode",
                        tint = if (voiceActive) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            // Send Button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                IconButton(
                    onClick = {
                        val clean = input.trim()
                        if (clean.isNotBlank()) {
                            input = ""
                            onSend(clean) {}
                        }
                    },
                    enabled = !sending,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_send),
                        contentDescription = "Send",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(
    profile: UserProfile,
    emotion: String,
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ana_avatar),
                contentDescription = "Ana",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ANA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${relationshipLabel(profile.relationshipStyle)} mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = emotionColor(emotion).copy(alpha = 0.14f),
            ) {
                Text(
                    text = emotion.replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = emotionColor(emotion),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EmptyChat(profile: UserProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ana is ready",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Try: Good morning, tell me a joke, or I had a bad day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            if (profile.birthday.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Birthday saved: ${profile.birthday}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isPlaying: Boolean = false,
    onTogglePlay: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 10.dp,
                bottomStart = if (isUser) 10.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 10.dp,
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            tonalElevation = if (isUser) 0.dp else 1.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.content,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isUser && onTogglePlay != null) {
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = if (isPlaying) R.drawable.ic_stop else R.drawable.ic_voice),
                                contentDescription = if (isPlaying) "Stop voice" else "Play voice",
                                modifier = Modifier.size(16.dp),
                                tint = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = "Delete chat",
                                modifier = Modifier.size(16.dp),
                                tint = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun configureSweetGirlVoice(tts: TextToSpeech) {
    val sweetVoice = tts.voices
        ?.filter { it.locale.language == Locale.ENGLISH.language }
        ?.sortedWith(
            compareByDescending<Voice> { it.name.containsAny("female", "woman", "girl") }
                .thenByDescending { it.name.containsAny("neural", "wavenet", "premium", "enhanced") }
                .thenByDescending { it.locale.country == Locale.US.country }
                .thenBy { it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
                .thenBy { it.latency }
        )
        ?.firstOrNull()

    if (sweetVoice != null) {
        tts.voice = sweetVoice
    } else {
        tts.language = Locale.US
    }

    tts.setSpeechRate(0.93f)
    tts.setPitch(1.06f)
}

private fun String.containsAny(vararg needles: String): Boolean {
    val text = lowercase(Locale.US)
    return needles.any { text.contains(it) }
}
