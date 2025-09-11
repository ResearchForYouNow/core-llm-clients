package io.github.researchforyounow.llm.error
import kotlinx.serialization.SerializationException

/**
 * Minimal error model for public API failures.
 * All library failures returned via Result.failure will be instances of LlmError.
 */
sealed class LlmError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class TransportError(
        message: String,
        cause: Throwable? = null,
    ) : LlmError(message, cause)

    class ProviderHttpError(
        val statusCode: Int,
        val body: String? = null,
        cause: Throwable? = null,
    ) : LlmError("Provider HTTP error: status=$statusCode", cause)

    class RateLimitError(
        val retryAfterSeconds: Long? = null,
        message: String = "Rate limited",
        cause: Throwable? = null,
    ) : LlmError(message, cause)

    class AuthError(
        message: String = "Authentication/Authorization failed",
        cause: Throwable? = null,
    ) : LlmError(message, cause)

    class DeserializationError(
        message: String = "Failed to parse provider response",
        cause: Throwable? = null,
    ) : LlmError(message, cause)

    class InvalidRequestError(
        message: String = "Invalid request",
        cause: Throwable? = null,
    ) : LlmError(message, cause)

    class ProviderSpecificError(
        val code: String? = null,
        message: String,
        cause: Throwable? = null,
    ) : LlmError(message, cause)

    companion object {
        fun fromException(
            e: Throwable,
        ): LlmError {
            if (e is LlmError) return e
            return when (e) {
                is java.net.SocketTimeoutException -> TransportError("Request timed out", e)

                is SerializationException -> DeserializationError(cause = e)

                else -> {
                    val msg = e.message ?: ""
                    when {
                        msg.contains("401") || msg.contains("unauthorized", true) -> AuthError(cause = e)
                        msg.contains("429") || msg.contains("rate limit", true) -> RateLimitError(cause = e)
                        msg.contains("400") || msg.contains("422") -> InvalidRequestError(cause = e)
                        msg.contains("OpenAI API error") || msg.contains("Gemini API error") ->
                            ProviderSpecificError(
                                message = msg,
                                cause = e,
                            )

                        else -> TransportError(
                            message = msg.ifBlank { "Unknown transport error" },
                            cause = e,
                        )
                    }
                }
            }
        }
    }
}
