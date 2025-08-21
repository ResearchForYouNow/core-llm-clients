package usage

/**
 * Normalized token usage information reported by LLM providers.
 *
 * @property promptTokens tokens consumed by the prompt
 * @property completionTokens tokens generated in the completion
 * @property totalTokens total tokens used, if available
 */
data class LlmUsage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
)
