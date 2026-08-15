package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiModel
import com.example.data.model.AiPersona
import com.example.data.model.NetworkHealthState
import com.example.data.model.ThemeMode
import com.example.ui.components.ManageMemorySheet
import com.example.ui.components.ModelSelectorSheet
import com.example.ui.components.PersonaSelectorSheet
import com.example.ui.theme.CrimsonOffline
import com.example.ui.theme.EmeraldOnline
import com.example.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var urlInput by remember(uiState.settings.customBaseUrl) { mutableStateOf(uiState.settings.customBaseUrl) }

    var showModelSheet by remember { mutableStateOf(false) }
    var showPersonaSheet by remember { mutableStateOf(false) }
    var showMemorySheet by remember { mutableStateOf(false) }

    val currentModelName = remember(uiState.settings.defaultModel) {
        AiModel.ALL_MODELS.find { it.id.equals(uiState.settings.defaultModel, ignoreCase = true) }?.name ?: "Auto Best"
    }

    val currentPersonaName = remember(uiState.settings.defaultPersona) {
        AiPersona.ALL_PERSONAS.find { it.id.equals(uiState.settings.defaultPersona, ignoreCase = true) }?.name ?: "General"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings & Configuration",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: Backend Connection
            item {
                SettingsSectionContainer(title = "BACKEND CONNECTION", icon = Icons.Default.Dns) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Next AI API Base URL",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Connect to your local FastAPI/Next AI server or custom deployed host.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("http://10.0.2.2:8000/") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backend_url_input")
                        )

                        // Preset shortcuts
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PresetUrlChip("Android Emulator (10.0.2.2:8000)") {
                                urlInput = "http://10.0.2.2:8000/"
                                viewModel.setBaseUrl(urlInput)
                            }
                            PresetUrlChip("Localhost (127.0.0.1:8000)") {
                                urlInput = "http://127.0.0.1:8000/"
                                viewModel.setBaseUrl(urlInput)
                            }
                        }

                        // Test & Save Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Health Status Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val statusColor = when (uiState.healthState) {
                                    is NetworkHealthState.Connected -> EmeraldOnline
                                    is NetworkHealthState.Checking -> Color(0xFFF59E0B)
                                    is NetworkHealthState.Offline -> CrimsonOffline
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                val statusLabel = when (val state = uiState.healthState) {
                                    is NetworkHealthState.Connected -> "Online (${state.latencyMs}ms)"
                                    is NetworkHealthState.Checking -> "Testing..."
                                    is NetworkHealthState.Offline -> "Offline"
                                }
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.setBaseUrl(urlInput)
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = !uiState.isTestingConnection,
                                modifier = Modifier.testTag("save_and_test_backend_button")
                            ) {
                                if (uiState.isTestingConnection) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Save & Test")
                            }
                        }
                    }
                }
            }

            // SECTION 2: Appearance & Theme
            item {
                SettingsSectionContainer(title = "APPEARANCE", icon = Icons.Default.DarkMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOptionRow(
                            label = "System Default",
                            selected = uiState.settings.themeMode == ThemeMode.SYSTEM,
                            onSelect = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            tag = "theme_system"
                        )
                        ThemeOptionRow(
                            label = "Clean Minimal Light",
                            selected = uiState.settings.themeMode == ThemeMode.LIGHT,
                            onSelect = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            tag = "theme_light"
                        )
                        ThemeOptionRow(
                            label = "Deep Minimal Dark",
                            selected = uiState.settings.themeMode == ThemeMode.DARK,
                            onSelect = { viewModel.setThemeMode(ThemeMode.DARK) },
                            tag = "theme_dark"
                        )
                    }
                }
            }

            // SECTION 3: AI Defaults & Routing
            item {
                SettingsSectionContainer(title = "AI DEFAULTS", icon = Icons.Default.AutoAwesome) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Default Model Setting
                        SettingClickableRow(
                            title = "Default AI Model",
                            subtitle = currentModelName,
                            icon = Icons.Default.Memory,
                            onClick = { showModelSheet = true },
                            tag = "default_model_row"
                        )

                        // Default Persona Setting
                        SettingClickableRow(
                            title = "Default Persona",
                            subtitle = currentPersonaName,
                            icon = Icons.Default.Psychology,
                            onClick = { showPersonaSheet = true },
                            tag = "default_persona_row"
                        )

                        // Web Search Default Toggle
                        SettingToggleRow(
                            title = "Enable Web Search by Default",
                            subtitle = "Perform live web searches for real-time information",
                            icon = Icons.Default.Language,
                            checked = uiState.settings.webSearchDefault,
                            onCheckedChange = viewModel::setWebSearchDefault,
                            tag = "default_web_search_switch"
                        )
                    }
                }
            }

            // SECTION 4: Memory & Long-term Context
            item {
                SettingsSectionContainer(title = "MEMORY & CONTEXT", icon = Icons.Default.Storage) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingClickableRow(
                            title = "Manage Next AI Memories",
                            subtitle = "View and add persistent context stored on the backend",
                            icon = Icons.Default.Storage,
                            onClick = {
                                viewModel.loadMemories()
                                showMemorySheet = true
                            },
                            tag = "manage_memories_row"
                        )
                    }
                }
            }

            // SECTION 5: Chat Preferences
            item {
                SettingsSectionContainer(title = "PREFERENCES", icon = Icons.Default.Schedule) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingToggleRow(
                            title = "Show Message Timestamps",
                            subtitle = "Display send time next to every message",
                            icon = Icons.Default.Schedule,
                            checked = uiState.settings.showTimestamps,
                            onCheckedChange = viewModel::setShowTimestamps,
                            tag = "show_timestamps_switch"
                        )

                        SettingToggleRow(
                            title = "Enter Key Sends Message",
                            subtitle = "Pressing enter on physical keyboard sends immediately",
                            icon = Icons.Default.Send,
                            checked = uiState.settings.enterToSend,
                            onCheckedChange = viewModel::setEnterToSend,
                            tag = "enter_to_send_switch"
                        )
                    }
                }
            }

            // SECTION 6: About
            item {
                SettingsSectionContainer(title = "ABOUT", icon = Icons.Default.Info) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Next AI for Android",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Version 1.1.0 • OmniRoute Engine",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "Features full SSE streaming, local memory persistence, markdown rendering, multimodal image analysis, and smart model routing.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }
    }

    // Modal sheets
    if (showModelSheet) {
        ModelSelectorSheet(
            selectedModelId = uiState.settings.defaultModel,
            onModelSelected = viewModel::setDefaultModel,
            onDismiss = { showModelSheet = false }
        )
    }

    if (showPersonaSheet) {
        PersonaSelectorSheet(
            selectedPersonaId = uiState.settings.defaultPersona,
            onPersonaSelected = viewModel::setDefaultPersona,
            onDismiss = { showPersonaSheet = false }
        )
    }

    if (showMemorySheet) {
        ManageMemorySheet(
            memories = uiState.memories,
            isLoading = uiState.isMemoryLoading,
            errorMessage = uiState.memoryError,
            successMessage = uiState.memorySuccessMessage,
            onLoadMemories = { viewModel.loadMemories() },
            onAddMemory = { viewModel.addMemory(it) },
            onDeleteMemory = { viewModel.deleteMemory(it) },
            onDismiss = {
                viewModel.clearMemoryMessages()
                showMemorySheet = false
            }
        )
    }
}

@Composable
private fun SettingsSectionContainer(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    )
                )
            }

            content()
        }
    }
}

@Composable
private fun PresetUrlChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun SettingClickableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
