package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ThemeMode
import com.example.ui.screens.MainScreen
import com.example.ui.theme.NextAiTheme
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (settingsState.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            NextAiTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    chatViewModel = chatViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

