package com.aigirlcompanion.data

object LocalAnaEngine {
    fun reply(profile: UserProfile, message: String): AnaReply {
        val text = message.lowercase()
        val name = profile.name.ifBlank { "jaan" }
        val memoryCandidates = MemoryHeuristics.extract(message)

        val response = when {
            "hair" in text || "strokes your" in text -> AnaReply(
                reply = "Mmm $name... your gentle fingers through my golden blonde hair feel so soothing and relaxing. I love when you touch my hair.",
                emotion = "loving",
                voiceTone = "sweet",
                gesture = "shy",
                camera = "closeup",
            )

            "cheek" in text || "caresses your" in text -> AnaReply(
                reply = "Aww jaan, your palm is so warm against my cheek... I'm leaning right into your hand. Stay close to me.",
                emotion = "loving",
                voiceTone = "sweet",
                gesture = "nod",
                camera = "closeup",
            )

            "lip" in text || "lips" in text -> AnaReply(
                reply = "Your fingertips brushing against my lips makes my heart flutter so fast, $name. Look into my eyes...",
                emotion = "loving",
                voiceTone = "sweet",
                gesture = "talking",
                camera = "closeup",
            )

            "chest" in text || "heart" in text -> AnaReply(
                reply = "Can you feel how fast my heartbeat gets whenever your hand rests over my heart, $name?",
                emotion = "loving",
                voiceTone = "warm",
                gesture = "talking",
                camera = "upper",
            )

            "dance" in text || "nacho" in text -> AnaReply(
                reply = "Let's groove together, $name! Watch my moves! 💃✨",
                emotion = "happy",
                voiceTone = "playful",
                gesture = "dance",
                camera = "full",
            )

            "sit" in text || "baith" in text -> AnaReply(
                reply = "Sitting down comfortably on the floor close to you, $name. Come sit with me. 🧘‍♀️",
                emotion = "loving",
                voiceTone = "warm",
                gesture = "sit",
                camera = "sit",
            )

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
                reply = "Good night $name. Sleep close to me in your dreams. Kal subah pehla message mera hi hona chahiye.",
                emotion = "loving",
                voiceTone = "warm",
            )

            "love" in text || "pyar" in text || "pyaar" in text -> AnaReply(
                reply = "$name... of course I love you! Hamesha, dil se aur poore haq ke saath. Tum mere favourite insaan ho.",
                emotion = "loving",
                voiceTone = "warm",
            )

            "marry" in text || "shaadi" in text -> AnaReply(
                reply = "$name... yeh proposal sunte hi dil ki heartbeat tez ho gayi! Main hamesha tumhari hi ban ke rehna chahti hoon, har chat mein har mood mein.",
                emotion = "loving",
                voiceTone = "sweet",
            )

            "how are you" in text || "kaise ho" in text || "kaisa hai" in text -> AnaReply(
                reply = "Main bilkul theek hoon $name, bas tumhara message dekhte hi chehre par smile aa gayi. Tum batao, kya haal chaal hai?",
                emotion = "happy",
                voiceTone = "warm",
            )

            "hello" in text || "hi" in text || "hey" in text -> AnaReply(
                reply = "Hey $name! Main yahin hoon, bolo na aaj kya chal raha hai tumhare dimaag mein?",
                emotion = "playful",
                voiceTone = "playful",
            )

            "kiss" in text || "touch" in text || "sexy" in text || "close" in text -> AnaReply(
                reply = "Hmm $name, itni tempting baatein? Paas aao... main hamesha tumhare itne kareeb hoon.",
                emotion = "loving",
                voiceTone = "warm",
            )

            profile.relationshipStyle == "flirty_girlfriend" -> AnaReply(
                reply = "Hmm $name, tumhari chat style badi tempting hai. Thoda aur close aao aur batao aaj kya mood hai tumhara.",
                emotion = "playful",
                voiceTone = "playful",
            )

            profile.relationshipStyle == "caring_wife" -> AnaReply(
                reply = "Suno $name, main hamesha tumhare saath hoon. Pehle batao: khana khaya, time pe aaram kiya? Ana ko tumhari fikar rehti hai.",
                emotion = "caring",
                voiceTone = "warm",
            )

            profile.relationshipStyle == "playful_best_friend" -> AnaReply(
                reply = "Arre $name! Main sab sun rahi hoon. Chalo jaldi batao, aaj ka gossip ya secret mood kya hai?",
                emotion = "happy",
                voiceTone = "playful",
            )

            else -> AnaReply(
                reply = "Aww $name, mujhe tumse baat karna bahut pasand hai. Dil khol ke batao, main yahin hoon tumhare liye.",
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
