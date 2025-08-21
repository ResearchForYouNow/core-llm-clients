package response

/**
 * Represents a chunk of streamed content from a provider.
 */
data class StreamChunk(
    val content: String,
)
