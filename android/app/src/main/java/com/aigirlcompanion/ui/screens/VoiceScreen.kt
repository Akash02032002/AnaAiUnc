package com.aigirlcompanion.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aigirlcompanion.R
import com.aigirlcompanion.data.ChatRepository
import com.aigirlcompanion.data.UserProfile
import com.aigirlcompanion.ui.components.AnaAvatar
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun VoiceScreen(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    sending: Boolean,
    lastEmotion: String,
    onSendVoice: (String, (String) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ChatRepository() }
    var status by remember { mutableStateOf("Tap the mic and speak to Ana.") }
    var ttsReady by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val tts = remember {
        TextToSpeech(context) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
        }
    }

    LaunchedEffect(ttsReady) {
        if (ttsReady) {
            configureSweetGirlVoice(tts)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            tts.stop()
            tts.shutdown()
        }
    }

    fun stopVoice() {
        player?.stop()
        player?.release()
        player = null
        tts.stop()
    }

    fun speakReply(reply: String) {
        stopVoice()
        status = "Ana is speaking..."
        scope.launch {
            val audioFile = File(context.cacheDir, "ana_voice_reply.mp3")
            val hasNeuralVoice = repository.synthesizeVoice(profile, reply, audioFile)
            if (hasNeuralVoice) {
                player = MediaPlayer().apply {
                    setDataSource(audioFile.absolutePath)
                    setOnCompletionListener {
                        it.release()
                        if (player == it) {
                            player = null
                        }
                        status = "Ana replied."
                    }
                    setOnErrorListener { mediaPlayer, _, _ ->
                        mediaPlayer.release()
                        if (player == mediaPlayer) {
                            player = null
                        }
                        if (ttsReady) {
                            tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "ana_reply")
                        }
                        status = "Ana replied."
                        true
                    }
                    prepare()
                    start()
                }
            } else {
                if (ttsReady) {
                    tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "ana_reply")
                }
                status = "Ana replied."
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
                status = "You said: $spoken"
                onSendVoice(spoken) { reply ->
                    speakReply(reply)
                }
            }
        } else {
            status = "Voice input cancelled."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Ana")
            }
            speechLauncher.launch(intent)
        } else {
            status = "Microphone permission is needed for voice mode."
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnaAvatar(emotion = lastEmotion)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Voice Mode",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Using free device speech and TTS first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (sending) "Ana is thinking..." else status,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (profile.backendUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Backend: ${profile.backendUrl}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    enabled = !sending,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mic),
                        contentDescription = null,
                    )
                    Text(
                        text = "Speak",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(
                    onClick = {
                        stopVoice()
                        status = "Stopped speaking."
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_stop),
                        contentDescription = null,
                    )
                    Text(
                        text = "Stop",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    speakReply("Hi Tittu, I am Ana. I will speak softly, sweetly, and close to a real human voice.")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_voice),
                    contentDescription = null,
                )
                Text(
                    text = "Preview Voice",
                    modifier = Modifier.padding(start = 8.dp),
                )
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

    tts.setSpeechRate(0.88f)
    tts.setPitch(1.18f)
}

private fun String.containsAny(vararg needles: String): Boolean {
    val text = lowercase(Locale.US)
    return needles.any { text.contains(it) }
}
