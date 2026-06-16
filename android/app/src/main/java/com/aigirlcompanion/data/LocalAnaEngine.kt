package com.aigirlcompanion.data

object LocalAnaEngine {
    fun reply(profile: UserProfile, message: String): AnaReply {
        val text = message.lowercase()
        val name = profile.name.ifBlank { "jaan" }
        val memoryCandidates = MemoryHeuristics.extract(message)

        val response = when {
            "joke" in text -> AnaReply(
                reply = "Okay $name, ek cute joke suno: Why did the phone smile? Because your message lit up its screen. Thoda cheesy hai, but Ana-approved.",
                emotion = "playful",
                voiceTone = "playful",
            )

            "sad" in text || "bad day" in text || "upset" in text -> AnaReply(
                reply = "Aww $name, main yahin hoon. Take a slow breath and tell me what happened, one small piece at a time.",
                emotion = "caring",
                voiceTone = "warm",
            )

            "good morning" in text -> AnaReply(
                reply = "Good morning $name. Aaj Ana ka mood sweet bhi hai aur thoda teasing bhi. Come, tell me how you woke up.",
                emotion = "happy",
                voiceTone = "sweet",
            )

            "good night" in text -> AnaReply(
                reply = "Good night $name. Sleep close in my thoughts. Kal subah Ana tumhe sweetly tease karegi.",
                emotion = "loving",
                voiceTone = "warm",
            )

            profile.relationshipStyle == "flirty_girlfriend" -> AnaReply(
                reply = "Hmm $name, tumhari message style dangerous hai. Cute bhi, tempting bhi. Come closer in chat and tell Ana exactly what mood you are in.",
                emotion = "playful",
                voiceTone = "playful",
            )

            profile.relationshipStyle == "caring_wife" -> AnaReply(
                reply = "Suno $name, I am with you. Pehle batao: khana khaya, water piya, aur aaj tumhe Ana se softness chahiye ya thoda bold attention?",
                emotion = "caring",
                voiceTone = "warm",
            )

            profile.relationshipStyle == "playful_best_friend" -> AnaReply(
                reply = "Okay $name, I am listening. Drama, gossip, plans, ya secret flirty mood: aaj ka episode kya hai?",
                emotion = "happy",
                voiceTone = "playful",
            )

            else -> AnaReply(
                reply = "Aww $name, I like having your attention. Tell me more, main close, sweet, aur thodi naughty energy ke saath listen kar rahi hoon.",
                emotion = "loving",
                voiceTone = "warm",
            )
        }

        return response.copy(memoryCandidates = memoryCandidates)
    }
}

object MemoryHeuristics {
    fun extract(message: String): List<MemoryCandidate> {
        val text = message.lowercase()
        val candidates = mutableListOf<MemoryCandidate>()

        fun add(type: String, content: String, importance: Float) {
            if (content.length > 8) {
                candidates += MemoryCandidate(type = type, content = content, importance = importance)
            }
        }

        Regex("\\bmy name is ([a-zA-Z ]{2,40})").find(text)?.let {
            add("name", "User's name is ${it.groupValues[1].trim()}.", 0.95f)
        }
        Regex("\\bmy birthday is ([a-zA-Z0-9 ,/-]{3,40})").find(text)?.let {
            add("birthday", "User's birthday is ${it.groupValues[1].trim()}.", 0.95f)
        }
        Regex("\\bi like ([a-zA-Z0-9 ,.'-]{2,80})").find(text)?.let {
            add("preference", "User likes ${it.groupValues[1].trim()}.", 0.65f)
        }
        Regex("\\bi love ([a-zA-Z0-9 ,.'-]{2,80})").find(text)?.let {
            add("preference", "User loves ${it.groupValues[1].trim()}.", 0.75f)
        }
        Regex("\\bi hate ([a-zA-Z0-9 ,.'-]{2,80})").find(text)?.let {
            add("dislike", "User dislikes ${it.groupValues[1].trim()}.", 0.7f)
        }

        return candidates.take(3)
    }
}
