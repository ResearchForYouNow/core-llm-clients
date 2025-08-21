package response

/**
 * Represents a single generated image result.
 * Either [url] or [b64Json] will be populated depending on response format.
 */
data class ImageResult(
    val url: String? = null,
    val b64Json: String? = null,
)
