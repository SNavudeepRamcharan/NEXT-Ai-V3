package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatRequest(
    @Json(name = "chat_id")
    val chatId: String,
    @Json(name = "messages")
    val messages: List<MessageSchema>,
    @Json(name = "model")
    val model: String = "auto/best",
    @Json(name = "image")
    val image: String? = null,
    @Json(name = "web_search")
    val webSearch: Boolean = false,
    @Json(name = "persona")
    val persona: String = "general"
)
