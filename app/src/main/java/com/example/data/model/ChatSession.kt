package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatSession(
    @Json(name = "chat_id")
    val id: String,
    @Json(name = "title")
    val title: String,
    @Json(name = "last_message")
    val lastMessage: String? = null,
    @Json(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "is_pinned")
    val isPinned: Boolean = false,
    @Json(name = "model")
    val model: String = "auto/best",
    @Json(name = "persona")
    val persona: String = "general"
)

@JsonClass(generateAdapter = true)
data class RenameChatRequest(
    @Json(name = "title")
    val title: String
)

@JsonClass(generateAdapter = true)
data class PinChatRequest(
    @Json(name = "is_pinned")
    val isPinned: Boolean
)
