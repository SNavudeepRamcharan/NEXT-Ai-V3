package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AiModel
import com.example.data.model.AiPersona
import com.example.data.model.ChatMessage
import com.example.data.model.MessageRole
import com.example.data.model.NetworkHealthState
import com.example.ui.components.MarkdownText
import com.example.ui.components.ModelSelectorSheet
import com.example.ui.components.PersonaSelectorSheet
import com.example.ui.theme.CrimsonOffline
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.MinimalLightSurfaceSubtle
import com.example.viewmodel.ChatUiState
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showModelSheet by remember { mutableStateOf(false) }
    var showPersonaSheet by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setImageUri(uri)
        }
    }

    // Auto-scroll when new messages arrive or during streaming
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    val modelName = remember(uiState.selectedModel) {
        AiModel.ALL_MODELS.find { it.id.equals(uiState.selectedModel, ignoreCase = true) }?.name ?: "Auto Best"
    }

    val personaName = remember(uiState.selectedPersona) {
        AiPersona.ALL_PERSONAS.find { it.id.equals(uiState.selectedPersona, ignoreCase = true) }?.name ?: "General"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Minimalist Header Bar (as in HTML design)
        HeaderSection(
            modelName = modelName,
            personaName = personaName,
            networkState = uiState.networkState,
            onModelClick = { showModelSheet = true },
            onPersonaClick = { showPersonaSheet = true },
            onNewChat = { viewModel.startNewChat() },
            onMenuClick = { showMenuDropdown = true },
            menuExpanded = showMenuDropdown,
            onDismissMenu = { showMenuDropdown = false },
            onRefreshHealth = { viewModel.checkHealth() }
        )

        // Main Message Stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.messages.isEmpty()) {
                EmptyChatWelcome(
                    modelName = modelName,
                    personaName = personaName,
                    onPromptClick = { prompt ->
                        viewModel.onInputTextChanged(prompt)
                    },
                    onOpenModelPicker = { showModelSheet = true }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onRegenerate = { viewModel.regenerateLastResponse() },
                            onStop = { viewModel.stopGeneration() }
                        )
                    }
                }
            }

            // Scroll to bottom floating helper if scrolled up
            val showScrollToBottom by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 2
                }
            }
            if (showScrollToBottom) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (uiState.messages.isNotEmpty()) {
                                listState.animateScrollToItem(uiState.messages.size - 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .testTag("scroll_to_bottom_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Input Section
        ComposerSection(
            inputText = uiState.inputText,
            onTextChanged = viewModel::onInputTextChanged,
            selectedImageUri = uiState.selectedImageUri,
            onClearImage = viewModel::clearImage,
            onPickImage = { imagePickerLauncher.launch("image/*") },
            webSearchEnabled = uiState.webSearchEnabled,
            onToggleWebSearch = viewModel::toggleWebSearch,
            isGenerating = uiState.isGenerating,
            onSend = viewModel::sendMessage,
            onStop = viewModel::stopGeneration
        )
    }

    // Model Selector Modal
    if (showModelSheet) {
        ModelSelectorSheet(
            selectedModelId = uiState.selectedModel,
            onModelSelected = viewModel::selectModel,
            onDismiss = { showModelSheet = false }
        )
    }

    // Persona Selector Modal
    if (showPersonaSheet) {
        PersonaSelectorSheet(
            selectedPersonaId = uiState.selectedPersona,
            onPersonaSelected = viewModel::selectPersona,
            onDismiss = { showPersonaSheet = false }
        )
    }
}

@Composable
private fun HeaderSection(
    modelName: String,
    personaName: String,
    networkState: NetworkHealthState,
    onModelClick: () -> Unit,
    onPersonaClick: () -> Unit,
    onNewChat: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onRefreshHealth: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Next AI brand avatar & title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "N" Logo Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                Column(
                    modifier = Modifier.clickable(onClick = onModelClick)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NEXT AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Network status indicator & model name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusDotColor = when (networkState) {
                            is NetworkHealthState.Connected -> EmeraldOnline
                            is NetworkHealthState.Checking -> Color(0xFFF59E0B)
                            is NetworkHealthState.Offline -> CrimsonOffline
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )

                        val statusText = when (networkState) {
                            is NetworkHealthState.Connected -> "Connected • $modelName"
                            is NetworkHealthState.Checking -> "Checking..."
                            is NetworkHealthState.Offline -> "Offline • $modelName"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            // Right action buttons: Persona chip, New chat, Overflow menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Persona button
                IconButton(
                    onClick = onPersonaClick,
                    modifier = Modifier.testTag("persona_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Select Persona ($personaName)",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // New Chat button
                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Overflow menu
                Box {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("chat_more_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = onDismissMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text("AI Model ($modelName)") },
                            onClick = {
                                onDismissMenu()
                                onModelClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Persona ($personaName)") },
                            onClick = {
                                onDismissMenu()
                                onPersonaClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Check Connection") },
                            onClick = {
                                onDismissMenu()
                                onRefreshHealth()
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onRegenerate: () -> Unit,
    onStop: () -> Unit
) {
    val isUser = message.role == MessageRole.USER
    val isAssistant = message.role == MessageRole.ASSISTANT
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Image attachment preview if present
        if (!message.imageUri.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(message.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Attached image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
        }

        // Bubble container
        if (isUser) {
            // User Bubble: Primary container background
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth(0.88f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 21.sp,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        } else {
            // Assistant Bubble: Surface Variant background
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .border(
                        width = if (message.isStreaming) 1.dp else 0.dp,
                        color = if (message.isStreaming) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Streaming pulse indicator
                    if (message.isStreaming) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "NEXT AI GENERATING...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }

                    if (message.content.isNotBlank()) {
                        MarkdownText(
                            markdown = message.content,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (message.isError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Generation failed or backend unreachable",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }
            }
        }

        // Timestamp and Assistant action chips (Stop / Regenerate)
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            )

            if (isAssistant) {
                if (message.isStreaming) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .clickable(onClick = onStop)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("stop_streaming_button")
                    ) {
                        Text(
                            text = "STOP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onRegenerate)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("regenerate_button")
                    ) {
                        Text(
                            text = "REGENERATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerSection(
    inputText: String,
    onTextChanged: (String) -> Unit,
    selectedImageUri: Uri?,
    onClearImage: () -> Unit,
    onPickImage: () -> Unit,
    webSearchEnabled: Boolean,
    onToggleWebSearch: () -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Selected Image Preview Pill
            if (selectedImageUri != null) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected image preview",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Image attached",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearImage,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("clear_attached_image")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Text input row with (+) button and (Send/Stop) button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Plus button for image attachment
                IconButton(
                    onClick = onPickImage,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Input Field
                TextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    placeholder = {
                        Text(
                            text = "Message Next AI...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp)
                        .testTag("chat_input_field"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Send or Stop button
                if (isGenerating) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .testTag("stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop generation",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = inputText.isNotBlank() || selectedImageUri != null,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() || selectedImageUri != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send message",
                            tint = if (inputText.isNotBlank() || selectedImageUri != null) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom row: Web Search toggle & feature indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Web Search Toggle Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (webSearchEnabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable(onClick = onToggleWebSearch)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("web_search_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = if (webSearchEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "WEB SEARCH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (webSearchEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Text(
                    text = "Markdown & Multi-turn",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyChatWelcome(
    modelName: String,
    personaName: String,
    onPromptClick: (String) -> Unit,
    onOpenModelPicker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "NEXT AI Assistant",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Running with $modelName • Persona: $personaName",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Suggested starter prompt chips
        val suggestions = listOf(
            "Explain how OmniRoute intelligent routing works",
            "Write a Python script for asynchronous web scraping",
            "Compare Gemini 2.5 Flash vs Nemotron 70B",
            "Help me write a concise engineering proposal"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPromptClick(suggestion) }
                        .testTag("starter_prompt_chip"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
