package io.github.researchforyounow.llm.providers.openai.request

/**
 * Represents an audio file payload for OpenAI audio endpoints.
 */
data class AudioFile(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String? = null,
) {
    init {
        require(bytes.isNotEmpty()) { "Audio file bytes cannot be empty" }
        require(fileName.isNotBlank()) { "Audio file name cannot be blank" }
    }
}
