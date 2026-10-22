package com.aigirlcompanion.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LocalMemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("ana_local_store", Context.MODE_PRIVATE)

    fun loadProfile(): UserProfile {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return UserProfile()
        return runCatching {
            val json = JSONObject(raw)
            UserProfile(
                name = json.optString("name", ""),
                birthday = json.optString("birthday", ""),
                relationshipStyle = json.optString("relationshipStyle", "flirty_girlfriend"),
                adultConfirmed = json.optBoolean("adultConfirmed", false),
                darkTheme = json.optBoolean("darkTheme", false),
                backendUrl = normalizeBackendUrl(json.optString("backendUrl", DEFAULT_BACKEND_URL)),
            )
        }.getOrDefault(UserProfile()).also { profile ->
            if (profile.backendUrl != jsonBackendUrl(raw)) {
                saveProfile(profile)
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        val json = JSONObject()
            .put("name", profile.name)
            .put("birthday", profile.birthday)
            .put("relationshipStyle", profile.relationshipStyle)
            .put("adultConfirmed", profile.adultConfirmed)
            .put("darkTheme", profile.darkTheme)
            .put("backendUrl", profile.backendUrl)
        prefs.edit().putString(KEY_PROFILE, json.toString()).apply()
    }

    fun loadMemories(): List<MemoryItem> {
        val raw = prefs.getString(KEY_MEMORIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        MemoryItem(
                            id = json.optString("id"),
                            type = json.optString("type", "note"),
                            content = json.optString("content"),
                            importance = json.optDouble("importance", 0.5).toFloat(),
                            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveMemories(memories: List<MemoryItem>) {
        val array = JSONArray()
        memories.forEach { memory ->
            array.put(
                JSONObject()
                    .put("id", memory.id)
                    .put("type", memory.type)
                    .put("content", memory.content)
                    .put("importance", memory.importance)
                    .put("createdAt", memory.createdAt)
            )
        }
        prefs.edit().putString(KEY_MEMORIES, array.toString()).apply()
    }

    fun loadMessages(): List<ChatMessage> {
        val raw = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val parsed = buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        ChatMessage(
                            id = json.optString("id"),
                            role = json.optString("role"),
                            content = json.optString("content"),
                            emotion = json.optString("emotion", "calm"),
                            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                        )
                    )
                }
            }
            val sanitized = parsed.filterNot(::isLegacyRestrictionMessage)
            if (sanitized.size != parsed.size) {
                saveMessages(sanitized)
            }
            sanitized
        }.getOrDefault(emptyList())
    }

    fun saveMessages(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.takeLast(80).forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("role", message.role)
                    .put("content", message.content)
                    .put("emotion", message.emotion)
                    .put("timestamp", message.timestamp)
            )
        }
        prefs.edit().putString(KEY_MESSAGES, array.toString()).commit()
    }

    fun removeLegacyRestrictionMessages() {
        val messages = loadMessages()
        val sanitized = messages.filterNot(::isLegacyRestrictionMessage)
        if (sanitized.size != messages.size) {
            saveMessages(sanitized)
        }
    }

    fun deleteAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_PROFILE = "profile"
        private const val KEY_MEMORIES = "memories"
        private const val KEY_MESSAGES = "messages"
    }
}

private fun normalizeBackendUrl(url: String): String {
    val clean = url.trim().trimEnd('/')
    if (clean.isBlank()) return DEFAULT_BACKEND_URL
    if (clean in LEGACY_BACKEND_URLS) return DEFAULT_BACKEND_URL
    return clean
}

private fun jsonBackendUrl(raw: String): String {
    return runCatching {
        JSONObject(raw).optString("backendUrl", DEFAULT_BACKEND_URL)
    }.getOrDefault(DEFAULT_BACKEND_URL)
}

private val LEGACY_BACKEND_URLS = setOf(
    "https://anaai-i2x9.onrender.com",
    "https://ana-ai-backend.onrender.com",
)
