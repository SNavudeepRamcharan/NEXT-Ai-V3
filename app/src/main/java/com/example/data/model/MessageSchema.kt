package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageSchema(
    @Json(name = "role")
    val role: String,
    @Json(name = "content")
    val content: String
)
