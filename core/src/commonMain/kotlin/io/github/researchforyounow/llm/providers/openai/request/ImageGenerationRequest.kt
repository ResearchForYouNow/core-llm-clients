package io.github.researchforyounow.llm.providers.openai.request

/**
 * Configuration for OpenAI image generation requests.
 */
data class ImageGenerationRequest(
    val prompt: String,
    val n: Int = 1,
    val size: String = "1024x1024",
    // e.g., "standard" or "hd" (model dependent)
    val quality: String? = null,
    val responseFormat: ImageResponseFormat = ImageResponseFormat.URL,
    // defaults applied in implementation
    val model: OpenAiImageModel? = null,
    val user: String? = null,
    val idempotencyKey: String? = null,
    val tags: Map<String, String>? = null,
) {
    init {
        require(prompt.isNotBlank()) { "Prompt cannot be blank" }
        require(n in 1..10) { "OpenAI supports generating between 1 and 10 images per request, got: $n" }
        require(size.matches(Regex("^\\d+x\\d+$"))) { "Size must be in the format WIDTHxHEIGHT, e.g., 1024x1024" }
    }
}

enum class ImageResponseFormat {
    URL,
    B64_JSON,
}
