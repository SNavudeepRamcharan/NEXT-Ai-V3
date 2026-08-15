package com.example

import com.example.data.model.AiModel
import com.example.data.model.AiPersona
import com.example.data.model.ModelCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextAiModelTests {

    @Test
    fun testAllModelsPresent() {
        val models = AiModel.ALL_MODELS
        assertTrue(models.isNotEmpty())
        assertTrue(models.any { it.category == ModelCategory.AUTO })
        assertTrue(models.any { it.category == ModelCategory.GEMINI })
        assertTrue(models.any { it.category == ModelCategory.NVIDIA })
    }

    @Test
    fun testFindModelById() {
        val autoModel = AiModel.findById("auto/best")
        assertEquals("Auto Best", autoModel.name)

        val flashModel = AiModel.findById("gemini/gemini-2.5-flash")
        assertEquals("Gemini 2.5 Flash", flashModel.name)
    }

    @Test
    fun testAllPersonasPresent() {
        val personas = AiPersona.ALL_PERSONAS
        assertTrue(personas.size >= 6)
        assertNotNull(personas.find { it.id == "coder" })
        assertNotNull(personas.find { it.id == "doctor" })
        assertNotNull(personas.find { it.id == "teacher" })
    }

    @Test
    fun testPersonaLookupFallback() {
        val unknown = AiPersona.findById("non_existent_persona")
        assertEquals("general", unknown.id)
    }
}
