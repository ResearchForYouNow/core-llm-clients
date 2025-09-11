package io.github.researchforyounow.llm.providers.gemini.request

import io.github.researchforyounow.llm.providers.gemini.config.GeminiConfig

/**
 * Request builder for Gemini API.
 * This class provides methods for building requests for the Gemini API.
 *
 * This class is internal and should not be used directly by consumers.
 */
internal class GeminiRequestBuilder private constructor(
    private val config: GeminiConfig,
) {
    /**
     * Builds a request for the Gemini API with combined instructions.
     * Gemini doesn't have separate system and user messages, so this method
     * combines them into a single instruction.
     *
     * @param instructions The combined instructions for the model
     * @return An ApiRequest object for the Gemini API
     */
    fun buildRequest(
        instructions: String,
    ): ApiRequest {
        return ApiRequest(
            contents = listOf(
                RequestContent(
                    parts = listOf(
                        RequestPart(text = instructions),
                    ),
                ),
            ),
            generationConfig = GenerationConfig(
                temperature = config.temperature,
                topK = config.topK,
                topP = config.topP,
                maxOutputTokens = config.maxOutputTokens,
                candidateCount = config.candidateCount,
                stopSequences = config.stopSequences,
            ),
        )
    }

    /**
     * Builds a request for the Gemini API with separate system message and prompt.
     * This method combines the system message and prompt into a single instruction.
     *
     * @param systemMessage The system message that provides context and instructions
     * @param prompt The user prompt or request
     * @return An ApiRequest object for the Gemini API
     */
    fun buildRequest(
        systemMessage: String,
        prompt: String,
    ): ApiRequest {
        // Combine system message and prompt for Gemini
        val instructions = "$systemMessage\n\n$prompt"
        return buildRequest(instructions)
    }

    companion object {
        /**
         * Creates a new GeminiRequestBuilder with the given configuration.
         */
        fun create(
            config: GeminiConfig,
        ): GeminiRequestBuilder {
            return GeminiRequestBuilder(config)
        }
    }
}
