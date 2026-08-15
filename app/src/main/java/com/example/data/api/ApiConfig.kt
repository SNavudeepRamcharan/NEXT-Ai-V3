package com.example.data.api

object ApiConfig {
    /**
     * Default backend base URL during development.
     * Note: 10.0.2.2 is the Android Emulator alias to host 127.0.0.1.
     * In production, this can be changed to https://your-domain.com/
     */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 120L // Extended for long streaming responses
    const val WRITE_TIMEOUT_SECONDS = 60L

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "http://$trimmed"
        } else {
            trimmed
        }
        return if (!withScheme.endsWith("/")) "$withScheme/" else withScheme
    }
}
