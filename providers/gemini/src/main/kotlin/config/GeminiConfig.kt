package config

import client.RetryPolicy
import usage.LlmUsageSink

/**
 * Configuration class for Gemini client.
 * This class contains all the configuration parameters specific to Gemini.
 *
 * This is the single configuration class that consumers use to create Gemini clients.
 * It includes both API parameters (apiKey, apiUrl) and generation parameters.
 */
data class GeminiConfig(
    val model: GeminiModel = GeminiModel.GEMINI_1_5_FLASH_LATEST,
    val temperature: Double = DEFAULT_TEMPERATURE,
    val topK: Int = DEFAULT_TOP_K,
    val topP: Double = DEFAULT_TOP_P,
    val maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS,
    val candidateCount: Int = DEFAULT_CANDIDATE_COUNT,
    val stopSequences: List<String> = emptyList(),
    val apiKey: String = "",
    val apiUrl: String = "",
    val retryPolicy: RetryPolicy = RetryPolicy.NO_RETRY,
    val usageSink: LlmUsageSink? = null,
    /**
     * Controls whether markdown fenced code blocks are stripped from Gemini responses.
     * When true (default), leading and trailing ``` fences (with optional language) are removed.
     * When false, raw content is returned including fences.
     */
    val cleanMarkdownCodeBlocks: Boolean = true,
) {
    init {
        require(model.modelName.isNotEmpty()) { "Model name cannot be empty" }
        require(temperature in 0.0..2.0) { "Temperature must be between 0.0 and 2.0, got: $temperature" }
        require(topK > 0) { "TopK must be positive, got: $topK" }
        require(topP in 0.0..1.0) { "TopP must be between 0.0 and 1.0, got: $topP" }
        require(maxOutputTokens > 0) { "MaxOutputTokens must be positive, got: $maxOutputTokens" }
        require(candidateCount > 0) { "CandidateCount must be positive, got: $candidateCount" }
    }

    /**
     * Creates a copy of this configuration with the specified API key.
     * Used internally by the factory to inject the API key.
     */
    fun withApiKey(
        apiKey: String,
    ): GeminiConfig {
        val finalApiUrl =
            this.apiUrl.ifEmpty {
                "https://generativelanguage.googleapis.com/v1beta/models/${model.modelName}:generateContent"
            }

        return copy(
            apiKey = apiKey,
            apiUrl = finalApiUrl,
        )
    }

    companion object {

        /**
         * Default temperature for Gemini.
         */
        const val DEFAULT_TEMPERATURE = 0.7

        /**
         * Default top-K for Gemini.
         */
        const val DEFAULT_TOP_K = 40

        /**
         * Default top-P for Gemini.
         */
        const val DEFAULT_TOP_P = 0.95

        /**
         * Default maximum output tokens for Gemini.
         */
        const val DEFAULT_MAX_OUTPUT_TOKENS = 2048

        /**
         * Default candidate count for Gemini.
         */
        const val DEFAULT_CANDIDATE_COUNT = 1

        /**
         * Creates a default configuration suitable for most use cases.
         */
        fun defaultConfig(): GeminiConfig = GeminiConfig()

        /**
         * Creates a configuration with custom model name and default generation parameters.
         */
        fun withModel(
            model: GeminiModel,
        ): GeminiConfig {
            return GeminiConfig(
                model = model,
            )
        }
    }
}
