package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiException
import com.example.data.model.ChatMessage
import com.example.data.model.MessageRole
import com.example.data.model.NetworkHealthState
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

data class ChatUiState(
    val activeChatId: String = UUID.randomUUID().toString(),
    val messages: List<ChatMessage> = emptyList(),
    val selectedModel: String = "auto/best",
    val selectedPersona: String = "general",
    val webSearchEnabled: Boolean = false,
    val selectedImageUri: Uri? = null,
    val selectedImageBase64: String? = null,
    val isGenerating: Boolean = false,
    val inputText: String = "",
    val networkState: NetworkHealthState = NetworkHealthState.Checking,
    val activeBackendUrl: String = "http://10.0.2.2:8000/",
    val errorMessage: String? = null
)

class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val chatRepository: ChatRepository = ChatRepository(),
    private val settingsRepository: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        // Collect settings and repository state
        viewModelScope.launch {
            combine(
                settingsRepository.settingsFlow,
                chatRepository.activeChatId,
                chatRepository.activeMessages
            ) { settings, activeId, activeMsgs ->
                Triple(settings, activeId, activeMsgs)
            }.collect { (settings, activeId, activeMsgs) ->
                _uiState.value = _uiState.value.copy(
                    activeChatId = activeId,
                    messages = activeMsgs,
                    activeBackendUrl = settings.customBaseUrl,
                    selectedModel = if (_uiState.value.selectedModel == "auto/best") settings.defaultModel else _uiState.value.selectedModel,
                    selectedPersona = if (_uiState.value.selectedPersona == "general") settings.defaultPersona else _uiState.value.selectedPersona,
                    webSearchEnabled = if (!_uiState.value.webSearchEnabled) settings.webSearchDefault else _uiState.value.webSearchEnabled
                )
            }
        }

        // Initial health check
        checkHealth()
    }

    fun checkHealth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(networkState = NetworkHealthState.Checking)
            val health = chatRepository.checkHealth(_uiState.value.activeBackendUrl)
            _uiState.value = _uiState.value.copy(networkState = health)
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText, errorMessage = null)
    }

    fun selectModel(modelId: String) {
        _uiState.value = _uiState.value.copy(selectedModel = modelId)
    }

    fun selectPersona(personaId: String) {
        _uiState.value = _uiState.value.copy(selectedPersona = personaId)
    }

    fun toggleWebSearch() {
        _uiState.value = _uiState.value.copy(webSearchEnabled = !_uiState.value.webSearchEnabled)
    }

    fun setImageUri(uri: Uri?) {
        if (uri == null) {
            _uiState.value = _uiState.value.copy(selectedImageUri = null, selectedImageBase64 = null)
            return
        }

        viewModelScope.launch {
            val base64 = encodeImageUriToBase64(uri)
            _uiState.value = _uiState.value.copy(
                selectedImageUri = uri,
                selectedImageBase64 = base64
            )
        }
    }

    fun clearImage() {
        _uiState.value = _uiState.value.copy(selectedImageUri = null, selectedImageBase64 = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun startNewChat() {
        stopGeneration()
        val newId = chatRepository.startNewChat(_uiState.value.selectedModel, _uiState.value.selectedPersona)
        _uiState.value = _uiState.value.copy(
            activeChatId = newId,
            selectedImageUri = null,
            selectedImageBase64 = null,
            inputText = "",
            errorMessage = null
        )
    }

    fun loadExistingChat(chatId: String, title: String? = null, model: String? = null, persona: String? = null) {
        stopGeneration()
        chatRepository.loadChat(
            chatId = chatId,
            title = title,
            model = model ?: _uiState.value.selectedModel,
            persona = persona ?: _uiState.value.selectedPersona
        )
        _uiState.value = _uiState.value.copy(
            activeChatId = chatId,
            selectedModel = model ?: _uiState.value.selectedModel,
            selectedPersona = persona ?: _uiState.value.selectedPersona,
            selectedImageUri = null,
            selectedImageBase64 = null,
            inputText = "",
            errorMessage = null
        )
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        val imageBase64 = _uiState.value.selectedImageBase64
        val imageUri = _uiState.value.selectedImageUri

        if (prompt.isEmpty() && imageBase64 == null) {
            return
        }

        if (_uiState.value.isGenerating) {
            return
        }

        // Append user message immediately
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = prompt.ifEmpty { "Analyze attached image" },
            imageUri = imageUri?.toString()
        )
        chatRepository.addMessage(userMessage)

        // Clear composer state immediately
        _uiState.value = _uiState.value.copy(
            inputText = "",
            selectedImageUri = null,
            selectedImageBase64 = null,
            isGenerating = true,
            errorMessage = null
        )

        // Prepare placeholder assistant message
        val assistantMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true,
            model = _uiState.value.selectedModel,
            persona = _uiState.value.selectedPersona
        )
        chatRepository.addMessage(assistantMessage)

        executeStreamingRequest(
            prompt = prompt,
            imageBase64 = imageBase64,
            model = _uiState.value.selectedModel,
            persona = _uiState.value.selectedPersona,
            webSearch = _uiState.value.webSearchEnabled
        )
    }

    fun regenerateLastResponse() {
        if (_uiState.value.isGenerating) return

        val messages = _uiState.value.messages
        if (messages.isEmpty()) return

        // Remove last assistant message if exists
        if (messages.last().role == MessageRole.ASSISTANT) {
            chatRepository.removeLastAssistantMessage()
        }

        val updatedMessages = chatRepository.activeMessages.value
        val lastUserMsg = updatedMessages.lastOrNull { it.role == MessageRole.USER } ?: return

        // Add assistant placeholder
        val assistantMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true,
            model = _uiState.value.selectedModel,
            persona = _uiState.value.selectedPersona
        )
        chatRepository.addMessage(assistantMessage)
        _uiState.value = _uiState.value.copy(isGenerating = true, errorMessage = null)

        executeStreamingRequest(
            prompt = lastUserMsg.content,
            imageBase64 = null,
            model = _uiState.value.selectedModel,
            persona = _uiState.value.selectedPersona,
            webSearch = _uiState.value.webSearchEnabled
        )
    }

    private fun executeStreamingRequest(
        prompt: String,
        imageBase64: String?,
        model: String,
        persona: String,
        webSearch: Boolean
    ) {
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            val baseUrl = _uiState.value.activeBackendUrl
            try {
                chatRepository.sendChatStream(
                    baseUrl = baseUrl,
                    prompt = prompt,
                    imageBase64 = imageBase64,
                    webSearch = webSearch,
                    model = model,
                    persona = persona
                ).catch { error ->
                    Log.e(TAG, "Streaming catch block: ${error.message}", error)
                    val friendlyMsg = when (error) {
                        is ApiException -> error.message ?: "Server error (${error.code})"
                        else -> "Next AI couldn't connect to the server. Check your connection or backend URL in Settings."
                    }
                    chatRepository.markLastAssistantError(friendlyMsg)
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        errorMessage = friendlyMsg
                    )
                }.collect { chunk ->
                    chatRepository.updateLastAssistantMessage(newChunk = chunk, isComplete = false)
                }

                // Streaming finished cleanly
                chatRepository.setLastAssistantComplete()
                _uiState.value = _uiState.value.copy(isGenerating = false)
            } catch (e: Exception) {
                Log.e(TAG, "Streaming execution error: ${e.message}", e)
                val friendly = "Generation stopped or encountered an error."
                chatRepository.markLastAssistantError(friendly)
                _uiState.value = _uiState.value.copy(isGenerating = false, errorMessage = friendly)
            }
        }
    }

    fun stopGeneration() {
        if (_uiState.value.isGenerating) {
            streamingJob?.cancel()
            streamingJob = null
            chatRepository.setLastAssistantComplete()
            _uiState.value = _uiState.value.copy(isGenerating = false)
            Log.i(TAG, "AI Generation stopped by user.")
        }
    }

    private suspend fun encodeImageUriToBase64(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = getApplication<Application>().contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Resize if oversized to preserve memory and bandwidth
                val scaled = if (bitmap.width > 1200 || bitmap.height > 1200) {
                    val ratio = Math.min(1200f / bitmap.width, 1200f / bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * ratio).toInt(),
                        (bitmap.height * ratio).toInt(),
                        true
                    )
                } else {
                    bitmap
                }

                val byteArrayOutputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image to base64: ${e.message}")
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }
}
