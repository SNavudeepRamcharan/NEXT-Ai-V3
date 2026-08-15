package com.example.data.model

enum class ModelCategory(val title: String) {
    AUTO("Auto Routing"),
    GEMINI("Google Gemini"),
    NVIDIA("NVIDIA Nemotron")
}

data class AiModel(
    val id: String,
    val name: String,
    val category: ModelCategory,
    val description: String,
    val isVisionCapable: Boolean = false,
    val isFast: Boolean = false,
    val badge: String? = null
) {
    companion object {
        val ALL_MODELS = listOf(
            // Auto routing
            AiModel(
                id = "auto/best",
                name = "Auto Best",
                category = ModelCategory.AUTO,
                description = "OmniRoute intelligent auto routing based on prompt complexity",
                badge = "Recommended"
            ),
            AiModel(
                id = "auto/best-fast",
                name = "Auto Fast",
                category = ModelCategory.AUTO,
                description = "Fastest low-latency model selection for quick queries",
                isFast = true,
                badge = "Ultra Fast"
            ),
            AiModel(
                id = "auto/best-coding",
                name = "Auto Coding",
                category = ModelCategory.AUTO,
                description = "Specialized routing for coding, architecture & debugging",
                badge = "Code"
            ),
            AiModel(
                id = "auto/best-reasoning",
                name = "Auto Reasoning",
                category = ModelCategory.AUTO,
                description = "Deep multi-step reasoning & structured logic",
                badge = "Reasoning"
            ),
            AiModel(
                id = "auto/best-vision",
                name = "Auto Vision",
                category = ModelCategory.AUTO,
                description = "Optimized multimodal vision & diagram understanding",
                isVisionCapable = true,
                badge = "Vision"
            ),

            // Gemini
            AiModel(
                id = "gemini/gemini-3-flash-preview",
                name = "Gemini 3 Flash Preview",
                category = ModelCategory.GEMINI,
                description = "Google's next-gen high efficiency frontier model",
                isVisionCapable = true,
                badge = "Preview"
            ),
            AiModel(
                id = "gemini/gemini-2.5-flash",
                name = "Gemini 2.5 Flash",
                category = ModelCategory.GEMINI,
                description = "High-performance multimodal model for general tasks",
                isVisionCapable = true
            ),
            AiModel(
                id = "gemini/gemini-2.5-flash-lite",
                name = "Gemini 2.5 Flash Lite",
                category = ModelCategory.GEMINI,
                description = "Ultra-lightweight fast response model",
                isFast = true
            ),
            AiModel(
                id = "gemini/gemini-3.1-flash-lite",
                name = "Gemini 3.1 Flash Lite",
                category = ModelCategory.GEMINI,
                description = "Next-generation lightweight model for quick tasks",
                isFast = true
            ),

            // NVIDIA
            AiModel(
                id = "nvidia/nvidia/nemotron-3-ultra-550b-a55b",
                name = "NVIDIA Nemotron 3 Ultra",
                category = ModelCategory.NVIDIA,
                description = "550B flagship frontier model for complex domains",
                badge = "550B"
            ),
            AiModel(
                id = "nvidia/nvidia/nemotron-3-super-120b-a12b",
                name = "NVIDIA Nemotron 3 Super",
                category = ModelCategory.NVIDIA,
                description = "120B high-capacity reasoning and synthesis model",
                badge = "120B"
            ),
            AiModel(
                id = "nvidia/nvidia/nemotron-3-nano-30b-a3b",
                name = "NVIDIA Nemotron 3 Nano",
                category = ModelCategory.NVIDIA,
                description = "30B efficient fast model for responsive conversations",
                isFast = true,
                badge = "30B"
            )
        )

        fun findById(id: String): AiModel {
            return ALL_MODELS.find { it.id.equals(id, ignoreCase = true) }
                ?: AiModel(
                    id = id,
                    name = id.substringAfterLast('/'),
                    category = ModelCategory.AUTO,
                    description = "Custom backend model: $id"
                )
        }
    }
}
