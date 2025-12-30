package io.github.researchforyounow.llm.providers.openai.client

import io.github.researchforyounow.llm.client.LlmClient
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.request.ImageGenerationRequest
import io.github.researchforyounow.llm.providers.openai.response.ImageResult
import io.ktor.client.HttpClient

/**
 * Interface for OpenAI-specific client.
 * This interface extends the base LlmClient interface.
 * OpenAI supports separate system and user messages, as well as temperature and maxTokens parameters,
 * which can all be specified in the generate method.
 */
interface OpenAiClient : LlmClient {
    /**
     * Generates image(s) using OpenAI Images API.
     * Returns either URLs or base64-encoded JSON depending on request.responseFormat.
     */
    suspend fun generateImage(
        request: ImageGenerationRequest,
    ): Result<List<ImageResult>>

    /**
     * Builder interface for creating OpenAiClient instances.
     */
    interface Builder {
        /**
         * Sets the HTTP client.
         */
        fun httpClient(
            httpClient: HttpClient,
        ): Builder

        /**
         * Sets the configuration.
         */
        fun config(
            config: OpenAiConfig,
        ): Builder

        /**
         * Builds the OpenAiClient instance.
         */
        fun build(): OpenAiClient
    }

    companion object {
        /**
         * Creates a new builder for OpenAiClient.
         */
        fun builder(): Builder = OpenAiClientImpl.Companion.builder()
    }
}
