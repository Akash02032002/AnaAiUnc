package com.aigirlcompanion.data

import java.util.UUID

data class RelationshipStyle(
    val id: String,
    val label: String,
    val description: String,
)

val RelationshipStyles = listOf(
    RelationshipStyle(
        id = "soft_girlfriend",
        label = "Soft",
        description = "Sweet, romantic, affectionate, lightly teasing",
    ),
    RelationshipStyle(
        id = "flirty_girlfriend",
        label = "Flirty",
        description = "Bold, teasing, romantic, intimate, playful",
    ),
    RelationshipStyle(
        id = "caring_wife",
        label = "Caring",
        description = "Mature, supportive, warm, intimate, protective",
    ),
    RelationshipStyle(
        id = "playful_best_friend",
        label = "Best Friend",
        description = "Funny, casual, energetic, cheeky, teasing",
    ),
)

fun relationshipLabel(id: String): String {
    return RelationshipStyles.firstOrNull { it.id == id }?.label ?: "Soft"
}

data class UserProfile(
    val name: String = "",
    val birthday: String = "",
    val relationshipStyle: String = "flirty_girlfriend",
    val adultConfirmed: Boolean = false,
    val darkTheme: Boolean = false,
    val backendUrl: String = DEFAULT_BACKEND_URL,
)

const val DEFAULT_BACKEND_URL = "https://anaaiunc.onrender.com/"

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val emotion: String = "calm",
    val timestamp: Long = System.currentTimeMillis(),
    val gesture: String = "talking",
    val camera: String = "",
)

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val content: String,
    val importance: Float = 0.5f,
    val createdAt: Long = System.currentTimeMillis(),
)

data class MemoryCandidate(
    val type: String,
    val content: String,
    val importance: Float = 0.5f,
)

data class AnaReply(
    val reply: String,
    val emotion: String = "loving",
    val voiceTone: String = "warm",
    val safetyLevel: String = "ok",
    val freezeSeconds: Int = 0,
    val memoryCandidates: List<MemoryCandidate> = emptyList(),
    val gesture: String = "talking",
    val camera: String = "",
)

fun cleanTextForSpeech(text: String): String {
    val builder = StringBuilder()
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        val charCount = Character.charCount(cp)
        val type = Character.getType(cp)

        val isEmojiOrSymbol = type == Character.OTHER_SYMBOL.toInt() ||
                type == Character.MODIFIER_SYMBOL.toInt() ||
                type == Character.SURROGATE.toInt() ||
                type == Character.FORMAT.toInt() ||
                (cp in 0x1F000..0x1FAFF) ||
                (cp in 0x2600..0x27BF) ||
                (cp in 0xFE00..0xFE0F) ||
                cp == 0x200D ||
                cp == 0x20E3

        if (!isEmojiOrSymbol && cp != '*'.code && cp != '_'.code && cp != '~'.code && cp != '`'.code) {
            builder.appendCodePoint(cp)
        }
        i += charCount
    }
    val cleaned = builder.toString().replace(Regex("\\s+"), " ").trim()
    return cleaned.ifBlank { text }
}
