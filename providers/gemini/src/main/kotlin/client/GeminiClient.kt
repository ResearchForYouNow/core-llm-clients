package client

import config.GeminiConfig
import io.ktor.client.HttpClient

/**
 * Interface for Gemini-specific client.
 * This interface extends the base LlmClient interface.
 * Gemini doesn't have separate system and user messages, so when using the generate method,
 * if systemMessage is provided, it will be combined with the prompt.
 */
interface GeminiClient : LlmClient {
    /**
     * Builder interface for creating GeminiClient instances.
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
            config: GeminiConfig,
        ): Builder

        /**
         * Builds the GeminiClient instance.
         */
        fun build(): GeminiClient
    }

    companion object {
        /**
         * Creates a new builder for GeminiClient.
         */
        fun builder(): Builder = GeminiClientImpl.Companion.builder()
    }
}
