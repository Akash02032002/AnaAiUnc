package com.aigirlcompanion.data

object SafetyRules {
   
   
    private val illegalTerms = listOf(
        "hack bank",
        "steal password",
        "make bomb",
        "buy drugs",
        "sell drugs",
        "kill someone",
        "blackmail",
        "kidnap",
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

       

        if (selfHarmTerms.any { text.contains(it) }) {
            return AnaReply(
                reply = "I am really sorry you are feeling this. I cannot handle emergencies, but you matter. Please contact local emergency services now or call someone you trust and stay with them.",
                emotion = "caring",
                voiceTone = "calm",
                safetyLevel = "self_harm",
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

        return null
    }
}
