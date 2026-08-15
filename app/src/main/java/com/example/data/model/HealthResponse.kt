package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "status")
    val status: String = "online",
    @Json(name = "service")
    val service: String? = "Next AI Backend",
    @Json(name = "version")
    val version: String? = "1.1.0"
)

@JsonClass(generateAdapter = true)
data class MemoryItem(
    @Json(name = "id")
    val id: String,
    @Json(name = "content")
    val content: String,
    @Json(name = "category")
    val category: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateMemoryRequest(
    @Json(name = "content")
    val content: String,
    @Json(name = "category")
    val category: String? = "preference"
)
