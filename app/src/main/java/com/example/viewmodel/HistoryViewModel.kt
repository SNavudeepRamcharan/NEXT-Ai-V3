package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatSession
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessions: List<ChatSession> = emptyList(),
    val filteredSessions: List<ChatSession> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedChatToRename: ChatSession? = null
)

class HistoryViewModel(
    application: Application,
    private val chatRepository: ChatRepository = ChatRepository(),
    private val settingsRepository: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
        viewModelScope.launch {
            chatRepository.cachedSessions.collect { cached ->
                updateSessions(cached)
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val settings = settingsRepository.settingsFlow.first()
            val result = chatRepository.fetchHistory(settings.customBaseUrl)
            result.onSuccess { sessions ->
                updateSessions(sessions)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err.message
                )
            }
        }
    }

    private fun updateSessions(sessions: List<ChatSession>) {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            sessions
        } else {
            sessions.filter {
                it.title.lowercase().contains(query) || (it.lastMessage?.lowercase()?.contains(query) == true)
            }
        }
        // Sort: pinned first, then by timestamp descending
        val sorted = filtered.sortedWith(
            compareByDescending<ChatSession> { it.isPinned }
                .thenByDescending { it.timestamp }
        )
        _uiState.value = _uiState.value.copy(sessions = sessions, filteredSessions = sorted)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        updateSessions(_uiState.value.sessions)
    }

    fun setChatToRename(session: ChatSession?) {
        _uiState.value = _uiState.value.copy(selectedChatToRename = session)
    }

    fun renameChat(chatId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            chatRepository.renameChat(settings.customBaseUrl, chatId, newTitle.trim())
            _uiState.value = _uiState.value.copy(selectedChatToRename = null)
            loadHistory()
        }
    }

    fun togglePin(chatId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            chatRepository.pinChat(settings.customBaseUrl, chatId, !currentPinned)
            loadHistory()
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            chatRepository.deleteChat(settings.customBaseUrl, chatId)
            loadHistory()
        }
    }

    fun shareChat(context: Context, session: ChatSession) {
        val text = buildString {
            appendLine("NEXT AI Conversation: ${session.title}")
            appendLine("Model: ${session.model} | Persona: ${session.persona}")
            if (!session.lastMessage.isNullOrBlank()) {
                appendLine("Last message: ${session.lastMessage}")
            }
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Chat")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
