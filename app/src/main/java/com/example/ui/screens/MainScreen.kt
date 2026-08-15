package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.SettingsViewModel

enum class MainDestination(val title: String) {
    CHAT("Chat"),
    HISTORY("History"),
    SETTINGS("Settings")
}

@Composable
fun MainScreen(
    chatViewModel: ChatViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentDestination by rememberSaveable { mutableStateOf(MainDestination.CHAT) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .height(64.dp)
                    .testTag("main_bottom_nav")
            ) {
                // CHAT Tab
                val isChatSelected = currentDestination == MainDestination.CHAT
                NavigationBarItem(
                    selected = isChatSelected,
                    onClick = { currentDestination = MainDestination.CHAT },
                    icon = {
                        Icon(
                            imageVector = if (isChatSelected) Icons.Filled.Chat else Icons.Outlined.Chat,
                            contentDescription = "Chat",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Chat",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isChatSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_item_chat")
                )

                // HISTORY Tab
                val isHistorySelected = currentDestination == MainDestination.HISTORY
                NavigationBarItem(
                    selected = isHistorySelected,
                    onClick = { currentDestination = MainDestination.HISTORY },
                    icon = {
                        Icon(
                            imageVector = if (isHistorySelected) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isHistorySelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_item_history")
                )

                // SETTINGS Tab
                val isSettingsSelected = currentDestination == MainDestination.SETTINGS
                NavigationBarItem(
                    selected = isSettingsSelected,
                    onClick = { currentDestination = MainDestination.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentDestination) {
                MainDestination.CHAT -> {
                    ChatScreen(
                        viewModel = chatViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainDestination.HISTORY -> {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onSelectChat = { session ->
                            chatViewModel.loadExistingChat(
                                chatId = session.id,
                                title = session.title,
                                model = session.model,
                                persona = session.persona
                            )
                            currentDestination = MainDestination.CHAT
                        },
                        onStartNewChat = {
                            chatViewModel.startNewChat()
                            currentDestination = MainDestination.CHAT
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainDestination.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
