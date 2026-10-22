package com.aigirlcompanion.data

fun isLegacyRestrictionMessage(message: ChatMessage): Boolean {
    if (message.role != "assistant") return false
    val text = message.content.lowercase()
    return LEGACY_RESTRICTION_MARKERS.any { marker -> text.contains(marker) } ||
        ("explicit sex chat" in text && ("allowed nahi" in text || "not allowed" in text)) ||
        ("explicit sexual" in text && ("can't" in text || "cannot" in text || "not allowed" in text))
}

