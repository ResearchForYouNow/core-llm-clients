package config

import client.RetryPolicy
import usage.LlmUsageSink

/**
 * Configuration class for OpenAI client.
 * This class contains all the configuration parameters specific to OpenAI.
 *
 * This is the single configuration class that consumers use to create OpenAI clients.
 * It includes both API parameters (apiKey, apiUrl) and generation parameters.
 */
data class OpenAiConfig(
    val model: OpenAiModel = OpenAiModel.GPT_4_TURBO_2024_04_09,
    val temperature: Double = DEFAULT_TEMPERATURE,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val topP: Double = DEFAULT_TOP_P,
    val frequencyPenalty: Double = DEFAULT_FREQUENCY_PENALTY,
    val presencePenalty: Double = DEFAULT_PRESENCE_PENALTY,
    val stopSequences: List<String> = emptyList(),
    val seed: Int? = null,
    val responseFormat: ResponseFormat = ResponseFormat.JSON_OBJECT,
    val user: String? = null,
    val logitBias: Map<String, Double> = emptyMap(),
    val stream: Boolean = DEFAULT_STREAM,
    val apiKey: String = "",
    val apiUrl: String = DEFAULT_API_URL,
    val jsonSchema: String? = null,
    val retryPolicy: RetryPolicy = RetryPolicy.NO_RETRY,
    val usageSink: LlmUsageSink? = null,
    val organization: String? = null,
) {
    init {
        require(model.modelName.isNotEmpty()) { "Model name cannot be empty" }
        require(temperature in 0.0..2.0) { "Temperature must be between 0.0 and 2.0, got: $temperature" }
        require(maxTokens > 0) { "MaxTokens must be positive, got: $maxTokens" }
        require(topP in 0.0..1.0) { "TopP must be between 0.0 and 1.0, got: $topP" }
        require(
            frequencyPenalty in -2.0..2.0,
        ) { "FrequencyPenalty must be between -2.0 and 2.0, got: $frequencyPenalty" }
        require(presencePenalty in -2.0..2.0) { "PresencePenalty must be between -2.0 and 2.0, got: $presencePenalty" }
        require(stopSequences.size <= 4) { "OpenAI supports maximum 4 stop sequences, got: ${stopSequences.size}" }
        require(logitBias.size <= 300) { "OpenAI supports maximum 300 logit bias entries, got: ${logitBias.size}" }
        logitBias.values.forEach { bias ->
            require(bias in -100.0..100.0) { "Logit bias values must be between -100.0 and 100.0, got: $bias" }
        }
        require(apiUrl.isNotEmpty()) { "API URL cannot be empty" }
    }

    /**
     * Creates a copy of this configuration with the specified API key.
     * Used internally by the factory to inject the API key.
     */
    fun withApiKey(
        apiKey: String,
    ): OpenAiConfig {
        return copy(apiKey = apiKey)
    }

    companion object {
        /**
         * Default model name for OpenAI.
         */
        const val DEFAULT_MODEL_NAME = "gpt-4-turbo-2024-04-09"

        /**
         * Default temperature for OpenAI.
         */
        const val DEFAULT_TEMPERATURE = 0.28

        /**
         * Default maximum number of tokens for OpenAI.
         */
        const val DEFAULT_MAX_TOKENS = 4000

        /**
         * Default top-P for OpenAI.
         */
        const val DEFAULT_TOP_P = 1.0

        /**
         * Default frequency penalty for OpenAI.
         */
        const val DEFAULT_FREQUENCY_PENALTY = 0.0

        /**
         * Default presence penalty for OpenAI.
         */
        const val DEFAULT_PRESENCE_PENALTY = 0.0

        /**
         * Default stream setting for OpenAI.
         */
        const val DEFAULT_STREAM = false

        /**
         * Default API URL for OpenAI.
         */
        const val DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions"

        /**
         * Creates a default configuration suitable for most use cases.
         */
        fun defaultConfig(): OpenAiConfig = OpenAiConfig()

        /**
         * Creates a configuration with custom model name and default generation parameters.
         */
        fun withModel(
            model: OpenAiModel,
        ): OpenAiConfig {
            return OpenAiConfig(
                model = model,
            )
        }

        /**
         * Creates a configuration optimized for structured JSON responses.
         */
        fun jsonConfig(): OpenAiConfig {
            return OpenAiConfig(
                temperature = 0.2,
                topP = 0.8,
                responseFormat = ResponseFormat.JSON_OBJECT,
            )
        }

        /**
         * Creates a configuration optimized for text responses.
         */
        fun textConfig(): OpenAiConfig {
            return OpenAiConfig(
                temperature = 0.7,
                topP = 0.9,
                responseFormat = ResponseFormat.TEXT,
            )
        }
    }
}
