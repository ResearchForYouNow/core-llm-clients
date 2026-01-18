package io.github.researchforyounow.llm.providers.openai.request

/**
 * Represents an audio file payload for OpenAI audio endpoints.
 */
data class AudioFile(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String? = null,
    val maxBytes: Int = DEFAULT_MAX_BYTES,
) {
    init {
        require(bytes.isNotEmpty()) { "Audio file bytes cannot be empty" }
        require(fileName.isNotBlank()) { "Audio file name cannot be blank" }
        require(bytes.size <= maxBytes) {
            "Audio file too large: ${bytes.size} bytes (max $maxBytes)"
        }
    }

    companion object {
        const val DEFAULT_MAX_BYTES: Int = 25 * 1024 * 1024
    }
}
