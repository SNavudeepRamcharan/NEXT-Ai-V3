package com.example.data.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultModel: String = "auto/best",
    val defaultPersona: String = "general",
    val webSearchDefault: Boolean = false,
    val customBaseUrl: String = "http://10.0.2.2:8000/",
    val enterToSend: Boolean = true,
    val showTimestamps: Boolean = true
)

sealed interface NetworkHealthState {
    data object Checking : NetworkHealthState
    data class Connected(val service: String, val version: String, val latencyMs: Long) : NetworkHealthState
    data class Offline(val reason: String) : NetworkHealthState
}
