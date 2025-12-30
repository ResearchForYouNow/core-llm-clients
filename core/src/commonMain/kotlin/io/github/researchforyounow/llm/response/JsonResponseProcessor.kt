package io.github.researchforyounow.llm.response

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.slf4j.Logger

/**
 * Interface for extracting content from provider-specific JSON responses.
 */
interface ResponseExtractor {
    /**
     * Extracts content from a JSON response.
     * @param responseJson The JSON response from the API
     * @return Result containing the extracted content string or an error
     */
    fun extractContent(
        responseJson: JsonObject,
    ): Result<String>
}

/**
 * Internal utility for processing JSON responses from different AI providers.
 *
 * This is an implementation detail used by provider modules (OpenAI, Gemini) to
 * extract provider-specific JSON content and deserialize it for callers.
 *
 * IMPORTANT: Not a public API. This type is not part of the library's supported
 * compatibility surface and may change without notice. Consumers should use the
 * high-level LlmClient APIs (e.g., generate/generateText/stream) instead.
 */
object JsonResponseProcessor {
    /**
     * Generic method for processing JSON responses from AI providers.
     *
     * This method uses a strategy pattern to extract content from provider-specific
     * JSON responses and then deserializes the content into the target class.
     *
     * @param responseJson The JSON response from the AI provider
     * @param targetClass The class to deserialize the response content into
     * @param contentExtractor Function to extract content from the provider-specific response
     * @param jsonParser The JSON parser to use for deserialization
     * @param logger The logger to use for logging
     * @return A Result containing the deserialized response or an error
     */
    fun <T> processResponse(
        responseJson: JsonObject,
        targetClass: Class<T>,
        contentExtractor: (JsonObject) -> Result<String>,
        jsonParser: Json,
        logger: Logger,
    ): Result<T> {
        logger.debug("Processing API response: {}", responseJson)

        return contentExtractor(responseJson)
            .mapCatching { content ->
                logger.debug("Extracted content length: {} characters", content.length)
                val serializer = jsonParser.serializersModule.serializer(targetClass)
                val result = jsonParser.decodeFromString(serializer, content)
                @Suppress("UNCHECKED_CAST")
                result as T
            }
            .onFailure { error ->
                logger.error("Error processing response", error)
            }
    }
}
