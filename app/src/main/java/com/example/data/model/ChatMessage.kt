package com.example.data.model

import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val model: String? = null,
    val persona: String? = null
)
