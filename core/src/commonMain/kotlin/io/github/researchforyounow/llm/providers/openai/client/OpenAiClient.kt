package io.github.researchforyounow.llm.providers.openai.client

import io.github.researchforyounow.llm.client.LlmClient
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.request.AudioTranscriptionRequest
import io.github.researchforyounow.llm.providers.openai.request.AudioTranslationRequest
import io.github.researchforyounow.llm.providers.openai.request.ImageGenerationRequest
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionConnection
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionSessionRequest
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionSessionResponse
import io.github.researchforyounow.llm.providers.openai.response.AudioTranscriptionResponse
import io.github.researchforyounow.llm.providers.openai.response.AudioTranscriptionStreamEvent
import io.github.researchforyounow.llm.providers.openai.response.ImageResult
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

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
     * Creates a transcription from the provided audio file.
     */
    suspend fun transcribe(
        request: AudioTranscriptionRequest,
    ): Result<AudioTranscriptionResponse>

    /**
     * Streams transcription events for the provided audio file.
     */
    fun streamTranscription(
        request: AudioTranscriptionRequest,
    ): Flow<AudioTranscriptionStreamEvent>

    /**
     * Translates the provided audio file into English.
     */
    suspend fun translate(
        request: AudioTranslationRequest,
    ): Result<AudioTranscriptionResponse>

    /**
     * Creates a realtime transcription session and returns an ephemeral client secret.
     */
    suspend fun createRealtimeTranscriptionSession(
        request: RealtimeTranscriptionSessionRequest,
    ): Result<RealtimeTranscriptionSessionResponse>

    /**
     * Opens a realtime transcription WebSocket connection.
     * If authToken is null, the OpenAI API key is used.
     */
    suspend fun openRealtimeTranscriptionConnection(
        request: RealtimeTranscriptionSessionRequest,
        authToken: String? = null,
    ): Result<RealtimeTranscriptionConnection>

    /**
     * Opens a realtime transcription WebSocket connection using a session
     * created via createRealtimeTranscriptionSession(...).
     */
    suspend fun openRealtimeTranscriptionConnection(
        request: RealtimeTranscriptionSessionRequest,
        session: RealtimeTranscriptionSessionResponse,
    ): Result<RealtimeTranscriptionConnection>

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
