package client

import kotlinx.coroutines.flow.Flow
import request.GenerationRequest
import response.StreamChunk

/**
 * Base interface for all LLM clients.
 * This interface defines the common methods that all LLM clients must implement.
 */
interface LlmClient {
    /**
     * Generates content using the LLM provider.
     * This simplified method uses a configuration object to encapsulate request parameters,
     * making the API cleaner and more maintainable.
     *
     * Generation parameters (temperature, maxTokens, etc.) are configured through
     * the client configuration that was provided when creating the client instance.
     *
     * @param request The generation request configuration containing prompt and optional system message
     * @param responseType The class of the expected response type
     * @return A Result containing the parsed response or an error
     */
    suspend fun <T> generate(
        request: GenerationRequest,
        responseType: Class<T>,
    ): Result<T>

    /**
     * Convenience helper for plain text responses.
     * Equivalent to generate(request, String::class.java).
     */
    suspend fun generateText(
        request: GenerationRequest,
    ): Result<String>

    /**
     * Returns the model name being used by this client.
     */
    fun getModelName(): String

    /**
     * Returns the API key being used by this client.
     */
    fun getApiKey(): String

    /**
     * Streams partial content using the LLM provider.
     * The returned [Flow] propagates cancellation to the underlying HTTP request
     * and emits [StreamChunk]s as they are received.
     */
    fun stream(
        request: GenerationRequest,
    ): Flow<StreamChunk>
}
