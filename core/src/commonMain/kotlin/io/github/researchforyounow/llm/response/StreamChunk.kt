package io.github.researchforyounow.llm.response

/**
 * Represents a chunk of streamed content from a provider.
 */
data class StreamChunk(
    val content: String,
)

/**
 * Represents a typed chunk of streamed content from a provider.
 * This allows streaming of structured JSON responses in addition to plain text.
 *
 * @param T The type of the parsed content
 * @param content The parsed content object of type T, or null if parsing failed for this chunk
 * @param rawContent The raw string content from the stream, always available for debugging
 * @param error Optional error message if parsing failed
 */
data class TypedStreamChunk<T>(
    val content: T?,
    val rawContent: String,
    val error: String? = null,
)
