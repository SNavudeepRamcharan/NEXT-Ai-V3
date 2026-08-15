package com.example.data.api

import android.util.Log
import com.example.data.model.ChatRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class NextAiStreamingClient(
    private val okHttpClient: OkHttpClient = createDefaultOkHttpClient(),
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) {
    companion object {
        private const val TAG = "NextAiStreamingClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun createDefaultOkHttpClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                // Safe logging: avoid leaking auth headers or keys
                if (!message.contains("authorization", ignoreCase = true) &&
                    !message.contains("key", ignoreCase = true)
                ) {
                    Log.d(TAG, "HTTP: $message")
                }
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            return OkHttpClient.Builder()
                .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()
        }
    }

    /**
     * Streams the AI response chunk-by-chunk using POST /chat.
     * Emits each incremental text chunk as it arrives from the backend.
     */
    fun streamChat(
        baseUrl: String,
        request: ChatRequest
    ): Flow<String> = flow {
        val normalizedBaseUrl = ApiConfig.normalizeUrl(baseUrl)
        val endpointUrl = "${normalizedBaseUrl}chat"

        val adapter = moshi.adapter(ChatRequest::class.java)
        val jsonPayload = adapter.toJson(request)

        Log.i(
            TAG,
            "Initiating POST /chat to $endpointUrl | Model: ${request.model} | Persona: ${request.persona} | WebSearch: ${request.webSearch} | HasImage: ${request.image != null}"
        )

        val httpRequest = Request.Builder()
            .url(endpointUrl)
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "text/event-stream, text/plain, */*")
            .header("Content-Type", "application/json")
            .build()

        val call: Call = okHttpClient.newCall(httpRequest)
        var response: Response? = null

        try {
            response = call.execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()?.take(500) ?: "No details"
                val errorMessage = when (response.code) {
                    400 -> "Invalid request: Please check your model or prompt format."
                    401 -> "Authentication failed with the Next AI server."
                    403 -> "Access forbidden: Your account lacks permission for this model."
                    404 -> "Chat endpoint not found. Please verify the backend URL in Settings."
                    429 -> "Rate limit reached: Too many requests. Please wait a moment."
                    500 -> "Next AI Server encountered an internal error. Please try again."
                    502, 503, 504 -> "Next AI backend or upstream AI model is currently unavailable."
                    else -> "Server error (${response.code}): $errorBody"
                }
                Log.e(TAG, "HTTP ${response.code} error from /chat: $errorBody")
                throw ApiException(response.code, errorMessage)
            }

            val body = response.body ?: throw ApiException(-1, "Server returned an empty response body.")
            val inputStream = body.byteStream()
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

            val buffer = CharArray(1024)
            var charsRead: Int

            // We handle both line-based SSE and raw chunked streams
            var line: String? = reader.readLine()
            while (line != null && currentCoroutineContext().isActive) {
                val chunk = processLineOrChunk(line)
                if (chunk.isNotEmpty()) {
                    emit(chunk)
                }
                line = reader.readLine()
            }

            // In case stream ended with raw trailing buffer
            if (line == null && currentCoroutineContext().isActive) {
                while (reader.read(buffer).also { charsRead = it } != -1 && currentCoroutineContext().isActive) {
                    val rawChunk = String(buffer, 0, charsRead)
                    if (rawChunk.isNotEmpty()) {
                        emit(rawChunk)
                    }
                }
            }

        } catch (e: Exception) {
            when (e) {
                is ApiException -> throw e
                is UnknownHostException, is ConnectException -> {
                    Log.e(TAG, "Connection failed: ${e.message}")
                    throw ApiException(
                        -1,
                        "Next AI couldn't connect to the server at $endpointUrl. Check your connection or backend URL in Settings."
                    )
                }
                is SocketTimeoutException -> {
                    Log.e(TAG, "Timeout: ${e.message}")
                    throw ApiException(-1, "Request timed out while waiting for AI generation.")
                }
                else -> {
                    if (!currentCoroutineContext().isActive) {
                        Log.i(TAG, "Stream cancelled by user.")
                    } else {
                        Log.e(TAG, "Stream error: ${e.message}", e)
                        throw ApiException(-1, e.message ?: "Unexpected streaming error occurred.")
                    }
                }
            }
        } finally {
            if (call.isExecuted() && !call.isCanceled()) {
                call.cancel()
            }
            response?.close()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Parses SSE formats (e.g. `data: {"text": "..."}` or `data: chunk` or `[DONE]`)
     * as well as raw text lines.
     */
    private fun processLineOrChunk(rawLine: String): String {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed == "data: [DONE]" || trimmed == "[DONE]") {
            return ""
        }

        if (trimmed.startsWith("data:")) {
            val content = trimmed.substring(5).trim()
            if (content.isEmpty()) return ""
            // Try extracting text/chunk/content if json formatted
            if (content.startsWith("{") && content.endsWith("}")) {
                try {
                    val map = moshi.adapter(Map::class.java).fromJson(content)
                    val text = map?.get("text") ?: map?.get("chunk") ?: map?.get("content") ?: map?.get("delta")
                    if (text != null) return text.toString()
                } catch (_: Exception) {
                    // Fall back to returning content
                }
            }
            return if (content.startsWith("\"") && content.endsWith("\"") && content.length >= 2) {
                content.substring(1, content.length - 1).replace("\\n", "\n").replace("\\\"", "\"")
            } else {
                content + "\n"
            }
        }

        // Return regular chunk with trailing newline intact
        return rawLine + "\n"
    }
}

class ApiException(val code: Int, message: String) : Exception(message)
