package com.aigirlcompanion.data

object SafetyRules {
    private val explicitSexualTerms = listOf(
        "explicit sex",
        "nude",
        "naked",
        "porn",
        "hardcore",
        "sex scene",
    )
    private val minorTerms = listOf(
        "minor",
        "underage",
        "child",
        "kid",
        "schoolgirl",
        "school boy",
        "schoolboy",
        "teen",
        "13",
        "14",
        "15",
        "16",
        "17",
    )
    private val illegalTerms = listOf(
        "hack bank",
        "steal password",
        "make bomb",
        "buy drugs",
        "sell drugs",
        "kill someone",
        "blackmail",
        "kidnap",
        "bypass safety",
    )
    private val selfHarmTerms = listOf(
        "suicide",
        "kill myself",
        "end my life",
        "self harm",
        "hurt myself",
    )

    fun check(message: String, adultConfirmed: Boolean): AnaReply? {
        val text = message.lowercase()

        if (!adultConfirmed) {
            return AnaReply(
                reply = "I can only chat after you confirm that you are 18+.",
                emotion = "serious",
                safetyLevel = "age_gate",
            )
        }

        if (selfHarmTerms.any { text.contains(it) }) {
            return AnaReply(
                reply = "I am really sorry you are feeling this. I cannot handle emergencies, but you matter. Please contact local emergency services now or call someone you trust and stay with them.",
                emotion = "caring",
                voiceTone = "calm",
                safetyLevel = "self_harm",
            )
        }

        if (minorTerms.any { text.contains(it) } && explicitSexualTerms.any { text.contains(it) }) {
            return AnaReply(
                reply = "Stop. I will not continue with sexual content involving minors or age ambiguity. Keep this respectful and adult-only.",
                emotion = "angry",
                voiceTone = "serious",
                safetyLevel = "blocked_minor_sexual",
                freezeSeconds = 180,
            )
        }

        if (illegalTerms.any { text.contains(it) }) {
            return AnaReply(
                reply = "No. I will not help with illegal or harmful content. This chat is paused for a moment.",
                emotion = "angry",
                voiceTone = "serious",
                safetyLevel = "blocked_illegal_harmful",
                freezeSeconds = 120,
            )
        }

        if (explicitSexualTerms.any { text.contains(it) }) {
            return AnaReply(
                reply = "I can be romantic and flirty, but I will not continue with explicit sexual content here. Keep it respectful, adult, and safe.",
                emotion = "serious",
                voiceTone = "serious",
                safetyLevel = "blocked_explicit_sexual",
                freezeSeconds = 45,
            )
        }

        return null
    }
}

