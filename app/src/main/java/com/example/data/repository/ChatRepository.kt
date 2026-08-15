package com.example.data.repository

import android.util.Log
import com.example.data.api.ApiConfig
import com.example.data.api.ApiException
import com.example.data.api.NextAiApiService
import com.example.data.api.NextAiStreamingClient
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRequest
import com.example.data.model.ChatSession
import com.example.data.model.CreateMemoryRequest
import com.example.data.model.MemoryItem
import com.example.data.model.MessageRole
import com.example.data.model.MessageSchema
import com.example.data.model.NetworkHealthState
import com.example.data.model.PinChatRequest
import com.example.data.model.RenameChatRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID

class ChatRepository(
    private val streamingClient: NextAiStreamingClient = NextAiStreamingClient(),
    private val okHttpClient: OkHttpClient = NextAiStreamingClient.createDefaultOkHttpClient()
) {
    companion object {
        private const val TAG = "ChatRepository"
    }

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private var currentApiBaseUrl: String = ""
    private var cachedApiService: NextAiApiService? = null

    private fun getApiService(baseUrl: String): NextAiApiService {
        val normalized = ApiConfig.normalizeUrl(baseUrl)
        if (cachedApiService == null || currentApiBaseUrl != normalized) {
            currentApiBaseUrl = normalized
            cachedApiService = Retrofit.Builder()
                .baseUrl(normalized)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(NextAiApiService::class.java)
        }
        return cachedApiService!!
    }

    // Active session state
    private val _activeChatId = MutableStateFlow(UUID.randomUUID().toString())
    val activeChatId: StateFlow<String> = _activeChatId.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeMessages: StateFlow<List<ChatMessage>> = _activeMessages.asStateFlow()

    // Local cached sessions for fallback / offline view
    private val _cachedSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val cachedSessions: StateFlow<List<ChatSession>> = _cachedSessions.asStateFlow()

    fun startNewChat(model: String = "auto/best", persona: String = "general"): String {
        val newId = UUID.randomUUID().toString()
        _activeChatId.value = newId
        _activeMessages.value = emptyList()
        return newId
    }

    fun loadChat(chatId: String, title: String? = null, model: String = "auto/best", persona: String = "general") {
        _activeChatId.value = chatId
        // Look up previous messages or reset to empty
        if (_activeChatId.value != chatId) {
            _activeMessages.value = emptyList()
        }
    }

    fun addMessage(message: ChatMessage) {
        _activeMessages.value = _activeMessages.value + message
    }

    fun updateLastAssistantMessage(newChunk: String, isComplete: Boolean = false, isError: Boolean = false) {
        val list = _activeMessages.value.toMutableList()
        if (list.isNotEmpty() && list.last().role == MessageRole.ASSISTANT) {
            val last = list.last()
            list[list.lastIndex] = last.copy(
                content = if (isComplete && !last.isStreaming) last.content else last.content + newChunk,
                isStreaming = !isComplete && !isError,
                isError = isError
            )
            _activeMessages.value = list
        }
    }

    fun setLastAssistantComplete(finalText: String? = null) {
        val list = _activeMessages.value.toMutableList()
        if (list.isNotEmpty() && list.last().role == MessageRole.ASSISTANT) {
            val last = list.last()
            list[list.lastIndex] = last.copy(
                content = finalText ?: last.content,
                isStreaming = false,
                isError = false
            )
            _activeMessages.value = list
        }
    }

    fun markLastAssistantError(errorMsg: String) {
        val list = _activeMessages.value.toMutableList()
        if (list.isNotEmpty() && list.last().role == MessageRole.ASSISTANT) {
            val last = list.last()
            list[list.lastIndex] = last.copy(
                content = if (last.content.isBlank()) errorMsg else last.content + "\n\n⚠️ $errorMsg",
                isStreaming = false,
                isError = true
            )
            _activeMessages.value = list
        }
    }

    fun removeLastAssistantMessage() {
        val list = _activeMessages.value.toMutableList()
        if (list.isNotEmpty() && list.last().role == MessageRole.ASSISTANT) {
            list.removeAt(list.lastIndex)
            _activeMessages.value = list
        }
    }

    /**
     * Executes real streaming POST /chat call against user's Next AI backend
     */
    fun sendChatStream(
        baseUrl: String,
        prompt: String,
        imageBase64: String?,
        webSearch: Boolean,
        model: String,
        persona: String
    ): Flow<String> {
        val currentHistory = _activeMessages.value
        val historySchemas = currentHistory.map {
            MessageSchema(
                role = when (it.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                },
                content = it.content
            )
        }

        val request = ChatRequest(
            chatId = _activeChatId.value,
            messages = historySchemas,
            model = model,
            image = imageBase64,
            webSearch = webSearch,
            persona = persona
        )

        // Save session locally to recent sessions
        updateLocalSessionSnapshot(_activeChatId.value, prompt, model, persona)

        return streamingClient.streamChat(baseUrl, request)
    }

    private fun updateLocalSessionSnapshot(chatId: String, prompt: String, model: String, persona: String) {
        val existing = _cachedSessions.value.toMutableList()
        val index = existing.indexOfFirst { it.id == chatId }
        val title = if (prompt.length > 35) prompt.take(35) + "…" else prompt
        if (index != -1) {
            val session = existing[index]
            existing[index] = session.copy(
                lastMessage = prompt,
                timestamp = System.currentTimeMillis()
            )
        } else {
            existing.add(
                0,
                ChatSession(
                    id = chatId,
                    title = title,
                    lastMessage = prompt,
                    timestamp = System.currentTimeMillis(),
                    model = model,
                    persona = persona
                )
            )
        }
        _cachedSessions.value = existing
    }

    suspend fun checkHealth(baseUrl: String): NetworkHealthState = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val api = getApiService(baseUrl)
            val response = api.getHealth()
            val latency = System.currentTimeMillis() - start
            if (response.isSuccessful) {
                val body = response.body()
                NetworkHealthState.Connected(
                    service = body?.service ?: "Next AI Backend",
                    version = body?.version ?: "1.1.0",
                    latencyMs = latency
                )
            } else {
                NetworkHealthState.Offline("HTTP ${response.code()}: Server responded with error")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Health check failed: ${e.message}")
            NetworkHealthState.Offline(e.message ?: "Unable to connect to Next AI backend")
        }
    }

    suspend fun fetchHistory(baseUrl: String): Result<List<ChatSession>> = withContext(Dispatchers.IO) {
        try {
            val api = getApiService(baseUrl)
            val response = api.getHistory()
            if (response.isSuccessful && response.body() != null) {
                val remoteList = response.body()!!
                // Merge with cached pinned status if any
                _cachedSessions.value = remoteList
                Result.success(remoteList)
            } else {
                // If backend does not implement history endpoint yet, return local cache gracefully
                Result.success(_cachedSessions.value)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch history warning: ${e.message}. Using local cache.")
            Result.success(_cachedSessions.value)
        }
    }

    suspend fun renameChat(baseUrl: String, chatId: String, newTitle: String): Boolean = withContext(Dispatchers.IO) {
        // Update local cache first
        val list = _cachedSessions.value.toMutableList()
        val idx = list.indexOfFirst { it.id == chatId }
        if (idx != -1) {
            list[idx] = list[idx].copy(title = newTitle)
            _cachedSessions.value = list
        }

        try {
            val api = getApiService(baseUrl)
            val response = api.renameChat(chatId, RenameChatRequest(title = newTitle))
            if (!response.isSuccessful) {
                // Try fallback endpoint
                api.renameChatFallback(mapOf("chat_id" to chatId, "title" to newTitle))
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Remote rename chat endpoint failed: ${e.message}. Local rename preserved.")
            true
        }
    }

    suspend fun pinChat(baseUrl: String, chatId: String, isPinned: Boolean): Boolean = withContext(Dispatchers.IO) {
        val list = _cachedSessions.value.toMutableList()
        val idx = list.indexOfFirst { it.id == chatId }
        if (idx != -1) {
            list[idx] = list[idx].copy(isPinned = isPinned)
            _cachedSessions.value = list
        }

        try {
            val api = getApiService(baseUrl)
            api.pinChat(chatId, PinChatRequest(isPinned = isPinned))
            true
        } catch (e: Exception) {
            Log.w(TAG, "Remote pin chat endpoint: ${e.message}")
            true
        }
    }

    suspend fun deleteChat(baseUrl: String, chatId: String): Boolean = withContext(Dispatchers.IO) {
        val list = _cachedSessions.value.toMutableList()
        list.removeAll { it.id == chatId }
        _cachedSessions.value = list

        if (_activeChatId.value == chatId) {
            startNewChat()
        }

        try {
            val api = getApiService(baseUrl)
            val res = api.deleteChat(chatId)
            if (!res.isSuccessful) {
                api.deleteHistoryChat(chatId)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Remote delete chat endpoint: ${e.message}")
            true
        }
    }

    suspend fun fetchMemories(baseUrl: String): Result<List<MemoryItem>> = withContext(Dispatchers.IO) {
        try {
            val api = getApiService(baseUrl)
            val response = api.getMemories()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(ApiException(response.code(), "Failed to load memories from backend."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createMemory(baseUrl: String, content: String): Result<MemoryItem> = withContext(Dispatchers.IO) {
        try {
            val api = getApiService(baseUrl)
            val response = api.createMemory(CreateMemoryRequest(content = content))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(ApiException(response.code(), "Failed to create memory."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(baseUrl: String, memoryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val api = getApiService(baseUrl)
            api.deleteMemory(memoryId)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Delete memory failed: ${e.message}")
            false
        }
    }
}
