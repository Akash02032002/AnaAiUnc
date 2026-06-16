package com.aigirlcompanion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ChatRepository {
    suspend fun sendMessage(
        profile: UserProfile,
        message: String,
        recentMessages: List<ChatMessage>,
        memories: List<MemoryItem>,
    ): AnaReply = withContext(Dispatchers.IO) {
        SafetyRules.check(message, profile.adultConfirmed)?.let { return@withContext it }

        runCatching {
            postToBackend(profile, message, recentMessages, memories)
        }.getOrElse {
            LocalAnaEngine.reply(profile, message).copy(safetyLevel = "offline_fallback")
        }
    }

    suspend fun synthesizeVoice(
        profile: UserProfile,
        text: String,
        outputFile: File,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
        val endpoint = profile.backendUrl.trimEnd('/') + "/v1/tts"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "audio/mpeg")
            }

            val payload = JSONObject()
                .put("text", text)
                .put("voice", "en-IN-NeerjaNeural")
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(payload.toString())
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                val raw = connection.errorStream?.bufferedReader()?.readText().orEmpty()
                connection.disconnect()
                throw IOException("TTS returned HTTP $code: $raw")
            }

            connection.inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()
            outputFile.length() > 0L
        }.getOrDefault(false)
    }

    private fun postToBackend(
        profile: UserProfile,
        message: String,
        recentMessages: List<ChatMessage>,
        memories: List<MemoryItem>,
    ): AnaReply {
        val endpoint = profile.backendUrl.trimEnd('/') + "/v1/chat"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        val payload = buildPayload(profile, message, recentMessages, memories)
        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(payload.toString())
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader()?.readText().orEmpty()
        connection.disconnect()

        if (code !in 200..299) {
            throw IOException("Backend returned HTTP $code: $raw")
        }

        val json = JSONObject(raw)
        return AnaReply(
            reply = json.optString("reply", "I am here with you."),
            emotion = json.optString("emotion", "loving"),
            voiceTone = json.optString("voice_tone", "warm"),
            safetyLevel = json.optString("safety_level", "ok"),
            freezeSeconds = json.optInt("freeze_seconds", 0),
            memoryCandidates = parseMemoryCandidates(json.optJSONArray("memory_candidates")),
        )
    }

    private fun buildPayload(
        profile: UserProfile,
        message: String,
        recentMessages: List<ChatMessage>,
        memories: List<MemoryItem>,
    ): JSONObject {
        val profileJson = JSONObject()
            .put("name", profile.name.ifBlank { "Friend" })
            .put("birthday", profile.birthday)
            .put("relationship_style", profile.relationshipStyle)
            .put("language_style", "Hinglish + English")
            .put("adult_confirmed", profile.adultConfirmed)

        val messagesJson = JSONArray()
        recentMessages
            .filterNot(::isLegacyRestrictionMessage)
            .takeLast(12)
            .forEach { chat ->
                messagesJson.put(
                    JSONObject()
                        .put("role", chat.role)
                        .put("content", chat.content)
                )
            }

        val memoriesJson = JSONArray()
        memories.take(30).forEach { memory ->
            memoriesJson.put(
                JSONObject()
                    .put("id", memory.id)
                    .put("type", memory.type)
                    .put("content", memory.content)
                    .put("importance", memory.importance)
            )
        }

        return JSONObject()
            .put("user_message", message)
            .put("profile", profileJson)
            .put("recent_messages", messagesJson)
            .put("memories", memoriesJson)
    }

    private fun parseMemoryCandidates(array: JSONArray?): List<MemoryCandidate> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val content = json.optString("content")
                if (content.isBlank()) continue
                add(
                    MemoryCandidate(
                        type = json.optString("type", "note"),
                        content = content,
                        importance = json.optDouble("importance", 0.5).toFloat(),
                    )
                )
            }
        }
    }
}
