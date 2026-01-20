package io.github.researchforyounow.llm.providers.openai.request

/**
 * Configuration for OpenAI audio translation requests.
 */
data class AudioTranslationRequest(
    val file: AudioFile,
    val model: String,
    val responseFormat: AudioResponseFormat = AudioResponseFormat.JSON,
    val prompt: String? = null,
    val temperature: Double? = null,
    val idempotencyKey: String? = null,
    val tags: Map<String, String>? = null,
) {
    init {
        require(model.isNotBlank()) { "Model name cannot be blank" }
    }
}
