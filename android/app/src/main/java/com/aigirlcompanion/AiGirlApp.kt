package com.aigirlcompanion

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.aigirlcompanion.data.AnaReply
import com.aigirlcompanion.data.ChatMessage
import com.aigirlcompanion.data.ChatRepository
import com.aigirlcompanion.data.LocalMemoryStore
import com.aigirlcompanion.data.MemoryCandidate
import com.aigirlcompanion.data.MemoryItem
import com.aigirlcompanion.data.UserProfile
import com.aigirlcompanion.ui.screens.ChatScreen
import com.aigirlcompanion.ui.screens.MemoryScreen
import com.aigirlcompanion.ui.screens.OnboardingScreen
import com.aigirlcompanion.ui.screens.ProfileScreen
import com.aigirlcompanion.ui.screens.SettingsScreen
import com.aigirlcompanion.ui.screens.VoiceScreen
import com.aigirlcompanion.ui.theme.AIGirlTheme
import kotlinx.coroutines.launch

private enum class AppTab(
    val label: String,
    val iconRes: Int,
) {
    Chat("Chat", R.drawable.ic_chat),
    Voice("Voice", R.drawable.ic_voice),
    Memory("Memory", R.drawable.ic_memory),
    Profile("Profile", R.drawable.ic_person),
    Settings("Settings", R.drawable.ic_settings),
}

@Composable
fun AiGirlApp() {
    val context = LocalContext.current
    val store = remember {
        LocalMemoryStore(context).also { it.removeLegacyRestrictionMessages() }
    }
    val repository = remember { ChatRepository() }
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf(store.loadProfile()) }
    var memories by remember { mutableStateOf(store.loadMemories()) }
    var messages by remember { mutableStateOf(store.loadMessages()) }
    var selectedTab by remember { mutableStateOf(AppTab.Chat) }
    var sending by remember { mutableStateOf(false) }
    var frozenUntil by remember { mutableLongStateOf(0L) }
    var lastEmotion by remember { mutableStateOf("loving") }

    fun saveProfile(updated: UserProfile) {
        profile = updated
        store.saveProfile(updated)
    }

    fun addMemoryCandidates(candidates: List<MemoryCandidate>) {
        val existing = memories.map { it.content.lowercase() }.toSet()
        val fresh = candidates
            .filter { it.content.isNotBlank() && it.content.lowercase() !in existing }
            .map {
                MemoryItem(
                    type = it.type,
                    content = it.content,
                    importance = it.importance,
                )
            }
        if (fresh.isNotEmpty()) {
            memories = (fresh + memories).take(120)
            store.saveMemories(memories)
        }
    }

    fun appendAssistant(reply: AnaReply, speak: (String) -> Unit = {}) {
        lastEmotion = reply.emotion
        val updated = messages + ChatMessage(
            role = "assistant",
            content = reply.reply,
            emotion = reply.emotion,
        )
        messages = updated
        store.saveMessages(updated)
        addMemoryCandidates(reply.memoryCandidates)
        if (reply.freezeSeconds > 0) {
            frozenUntil = System.currentTimeMillis() + reply.freezeSeconds * 1000L
        }
        speak(reply.reply)
    }

    fun sendUserMessage(text: String, speak: (String) -> Unit = {}) {
        val cleanText = text.trim()
        if (cleanText.isBlank() || sending) return

        val now = System.currentTimeMillis()
        if (now < frozenUntil) {
            val secondsLeft = ((frozenUntil - now) / 1000L).coerceAtLeast(1L)
            appendAssistant(
                AnaReply(
                    reply = "Ana is paused for $secondsLeft seconds because the last message crossed a safety boundary.",
                    emotion = "serious",
                    safetyLevel = "frozen",
                ),
                speak,
            )
            return
        }

        val userMessage = ChatMessage(role = "user", content = cleanText)
        val withUser = messages + userMessage
        messages = withUser
        store.saveMessages(withUser)

        scope.launch {
            sending = true
            val reply = repository.sendMessage(
                profile = profile,
                message = cleanText,
                recentMessages = withUser,
                memories = memories,
            )
            appendAssistant(reply, speak)
            sending = false
        }
    }

    AIGirlTheme(darkTheme = profile.darkTheme) {
        if (!profile.adultConfirmed) {
            OnboardingScreen(
                profile = profile,
                onComplete = { saveProfile(it.copy(adultConfirmed = true)) },
            )
            return@AIGirlTheme
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            val modifier = Modifier.padding(padding)
            when (selectedTab) {
                AppTab.Chat -> ChatScreen(
                    modifier = modifier,
                    profile = profile,
                    messages = messages,
                    sending = sending,
                    lastEmotion = lastEmotion,
                    onSend = ::sendUserMessage,
                )

                AppTab.Voice -> VoiceScreen(
                    modifier = modifier,
                    profile = profile,
                    sending = sending,
                    lastEmotion = lastEmotion,
                    onSendVoice = ::sendUserMessage,
                )

                AppTab.Memory -> MemoryScreen(
                    modifier = modifier,
                    memories = memories,
                    onDelete = { memory ->
                        memories = memories.filterNot { it.id == memory.id }
                        store.saveMemories(memories)
                    },
                    onClear = {
                        memories = emptyList()
                        store.saveMemories(memories)
                    },
                )

                AppTab.Profile -> ProfileScreen(
                    modifier = modifier,
                    profile = profile,
                    onProfileChange = ::saveProfile,
                )

                AppTab.Settings -> SettingsScreen(
                    modifier = modifier,
                    profile = profile,
                    onProfileChange = ::saveProfile,
                    onDeleteAll = {
                        store.deleteAll()
                        profile = UserProfile()
                        memories = emptyList()
                        messages = emptyList()
                        selectedTab = AppTab.Chat
                    },
                )
            }
        }
    }
}
