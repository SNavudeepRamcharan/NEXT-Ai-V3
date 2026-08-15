package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

data class AiPersona(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val systemPromptHint: String
) {
    companion object {
        val ALL_PERSONAS = listOf(
            AiPersona(
                id = "general",
                name = "General",
                subtitle = "Balanced AI Assistant",
                description = "Clear, accurate, and structured responses across all subjects.",
                icon = Icons.Default.AutoAwesome,
                systemPromptHint = "Balanced and helpful assistance."
            ),
            AiPersona(
                id = "coder",
                name = "Coder",
                subtitle = "Software Architect",
                description = "Clean code, architectural patterns, algorithmic optimization & bug fixing.",
                icon = Icons.Default.Code,
                systemPromptHint = "Write production-ready code with concise explanations."
            ),
            AiPersona(
                id = "teacher",
                name = "Teacher",
                subtitle = "Patient Educator",
                description = "Breaks down intricate topics step-by-step with analogies and examples.",
                icon = Icons.Default.School,
                systemPromptHint = "Explain intuitively from fundamentals to advanced details."
            ),
            AiPersona(
                id = "doctor",
                name = "Doctor",
                subtitle = "Medical Science Analyst",
                description = "Explains physiological principles, clinical research, and wellness data.",
                icon = Icons.Default.HealthAndSafety,
                systemPromptHint = "Objective medical insights with scientific context."
            ),
            AiPersona(
                id = "writer",
                name = "Writer",
                subtitle = "Prose & Copy Craftsman",
                description = "Compelling copywriting, narrative prose, editing, and stylistic polish.",
                icon = Icons.Default.EditNote,
                systemPromptHint = "Craft engaging prose with rhythm and clarity."
            ),
            AiPersona(
                id = "friend",
                name = "Friend",
                subtitle = "Empathetic Companion",
                description = "Warm, casual, conversational, and thoughtful dialogue partner.",
                icon = Icons.Default.Favorite,
                systemPromptHint = "Warm, empathetic, and relatable tone."
            )
        )

        fun findById(id: String): AiPersona {
            return ALL_PERSONAS.find { it.id.equals(id, ignoreCase = true) }
                ?: ALL_PERSONAS.first()
        }
    }
}
