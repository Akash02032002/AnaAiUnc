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
        description = "Sweet, romantic, gentle, emotionally warm",
    ),
    RelationshipStyle(
        id = "flirty_girlfriend",
        label = "Flirty",
        description = "Playful, teasing, romantic, respectful",
    ),
    RelationshipStyle(
        id = "caring_wife",
        label = "Caring",
        description = "Mature, supportive, warm, protective",
    ),
    RelationshipStyle(
        id = "playful_best_friend",
        label = "Best Friend",
        description = "Funny, casual, energetic, lightly flirty",
    ),
)

fun relationshipLabel(id: String): String {
    return RelationshipStyles.firstOrNull { it.id == id }?.label ?: "Soft"
}

data class UserProfile(
    val name: String = "",
    val birthday: String = "",
    val relationshipStyle: String = "soft_girlfriend",
    val adultConfirmed: Boolean = false,
    val darkTheme: Boolean = false,
    val backendUrl: String = "http://127.0.0.1:8000",
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val emotion: String = "calm",
    val timestamp: Long = System.currentTimeMillis(),
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
)
