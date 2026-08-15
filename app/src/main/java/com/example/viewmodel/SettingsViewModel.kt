package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiConfig
import com.example.data.model.AppSettings
import com.example.data.model.MemoryItem
import com.example.data.model.NetworkHealthState
import com.example.data.model.ThemeMode
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val healthState: NetworkHealthState = NetworkHealthState.Checking,
    val memories: List<MemoryItem> = emptyList(),
    val isTestingConnection: Boolean = false,
    val isMemoryLoading: Boolean = false,
    val memoryError: String? = null,
    val memorySuccessMessage: String? = null
)

class SettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val settingsRepository: SettingsRepository = SettingsRepository(application),
    private val chatRepository: ChatRepository = ChatRepository()
) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { currentSettings ->
                _uiState.value = _uiState.value.copy(settings = currentSettings)
                testConnection(currentSettings.customBaseUrl)
            }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(themeMode)
        }
    }

    fun setDefaultModel(modelId: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultModel(modelId)
        }
    }

    fun setDefaultPersona(personaId: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultPersona(personaId)
        }
    }

    fun setWebSearchDefault(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateWebSearchDefault(enabled)
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            val normalized = ApiConfig.normalizeUrl(url)
            settingsRepository.updateBaseUrl(normalized)
            testConnection(normalized)
        }
    }

    fun setEnterToSend(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEnterToSend(enabled)
        }
    }

    fun setShowTimestamps(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowTimestamps(enabled)
        }
    }

    fun testConnection(url: String? = null) {
        val targetUrl = url ?: _uiState.value.settings.customBaseUrl
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true)
            val health = chatRepository.checkHealth(targetUrl)
            _uiState.value = _uiState.value.copy(
                healthState = health,
                isTestingConnection = false
            )
        }
    }

    fun loadMemories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMemoryLoading = true, memoryError = null)
            val result = chatRepository.fetchMemories(_uiState.value.settings.customBaseUrl)
            result.onSuccess { items ->
                _uiState.value = _uiState.value.copy(
                    memories = items,
                    isMemoryLoading = false
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isMemoryLoading = false,
                    memoryError = err.message ?: "Unable to fetch memories from backend."
                )
            }
        }
    }

    fun addMemory(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMemoryLoading = true, memoryError = null)
            val result = chatRepository.createMemory(_uiState.value.settings.customBaseUrl, content.trim())
            result.onSuccess { newItem ->
                _uiState.value = _uiState.value.copy(
                    memories = listOf(newItem) + _uiState.value.memories,
                    isMemoryLoading = false,
                    memorySuccessMessage = "Memory saved to backend."
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isMemoryLoading = false,
                    memoryError = err.message ?: "Failed to save memory."
                )
            }
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            chatRepository.deleteMemory(_uiState.value.settings.customBaseUrl, memoryId)
            _uiState.value = _uiState.value.copy(
                memories = _uiState.value.memories.filter { it.id != memoryId }
            )
        }
    }

    fun clearMemoryMessages() {
        _uiState.value = _uiState.value.copy(memoryError = null, memorySuccessMessage = null)
    }
}
