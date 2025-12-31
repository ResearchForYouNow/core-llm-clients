package io.github.researchforyounow.llm.providers.openai.config

import io.github.researchforyounow.llm.client.RetryPolicy
import io.github.researchforyounow.llm.usage.LlmUsageSink

/**
 * Configuration class for OpenAI client.
 * This class contains all the configuration parameters specific to OpenAI.
 *
 * This is the single configuration class that consumers use to create OpenAI clients.
 * It includes both API parameters (apiKey, apiUrl) and generation parameters.
 */
data class OpenAiConfig(
    val modelName: String = DEFAULT_MODEL_NAME,
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
    val enableWebSearch: Boolean = false,
    val apiKey: String = "",
    val apiUrl: String = DEFAULT_API_URL,
    val jsonSchema: String? = null,
    val jsonSchemaName: String = DEFAULT_JSON_SCHEMA_NAME,
    val retryPolicy: RetryPolicy = RetryPolicy.NO_RETRY,
    val usageSink: LlmUsageSink? = null,
    val organization: String? = null,
    val streamParsingMode: StreamParsingMode = StreamParsingMode.NDJSON_PER_LINE,
    val ndjsonDelimiter: String = "\n",
) {
    init {
        require(modelName.isNotEmpty()) { "Model name cannot be empty" }
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
        require(jsonSchemaName.isNotBlank()) { "jsonSchemaName cannot be blank" }
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
        const val DEFAULT_MODEL_NAME = Models.GPT_4O_2024_08_06

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
         * Default JSON schema name for structured outputs.
         */
        const val DEFAULT_JSON_SCHEMA_NAME = "response"

        /**
         * Creates a default configuration suitable for most use cases.
         */
        fun defaultConfig(): OpenAiConfig = OpenAiConfig()

        /**
         * Creates a configuration with custom model name and default generation parameters.
         */
        @Deprecated(
            message = "Use withModel(modelName: String) or pass modelName in the constructor.",
            replaceWith = ReplaceWith("withModel(modelName)"),
        )
        fun withModel(model: OpenAiModel): OpenAiConfig = OpenAiConfig(modelName = model.modelName)

        fun withModel(modelName: String): OpenAiConfig = OpenAiConfig(modelName = modelName)

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

    @Deprecated(
        message = "Use primary constructor with modelName: String. OpenAiModel is deprecated.",
        replaceWith = ReplaceWith("OpenAiConfig(modelName = model.modelName)"),
    )
    constructor(model: OpenAiModel) : this(
        modelName = model.modelName,
    )

    @Deprecated(
        message = "Constructor signature updated to include enableWebSearch; use named arguments or primary constructor.",
        replaceWith = ReplaceWith(
            "OpenAiConfig(modelName, temperature, maxTokens, topP, frequencyPenalty, presencePenalty, " +
                "stopSequences, seed, responseFormat, user, logitBias, stream, enableWebSearch = false, " +
                "apiKey, apiUrl, jsonSchema, retryPolicy, usageSink, organization, streamParsingMode, ndjsonDelimiter)",
        ),
    )
    constructor(
        modelName: String = DEFAULT_MODEL_NAME,
        temperature: Double = DEFAULT_TEMPERATURE,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        topP: Double = DEFAULT_TOP_P,
        frequencyPenalty: Double = DEFAULT_FREQUENCY_PENALTY,
        presencePenalty: Double = DEFAULT_PRESENCE_PENALTY,
        stopSequences: List<String> = emptyList(),
        seed: Int? = null,
        responseFormat: ResponseFormat = ResponseFormat.JSON_OBJECT,
        user: String? = null,
        logitBias: Map<String, Double> = emptyMap(),
        stream: Boolean = DEFAULT_STREAM,
        apiKey: String = "",
        apiUrl: String = DEFAULT_API_URL,
        jsonSchema: String? = null,
        jsonSchemaName: String = DEFAULT_JSON_SCHEMA_NAME,
        retryPolicy: RetryPolicy = RetryPolicy.NO_RETRY,
        usageSink: LlmUsageSink? = null,
        organization: String? = null,
        streamParsingMode: StreamParsingMode = StreamParsingMode.NDJSON_PER_LINE,
        ndjsonDelimiter: String = "\n",
    ) : this(
        modelName = modelName,
        temperature = temperature,
        maxTokens = maxTokens,
        topP = topP,
        frequencyPenalty = frequencyPenalty,
        presencePenalty = presencePenalty,
        stopSequences = stopSequences,
        seed = seed,
        responseFormat = responseFormat,
        user = user,
        logitBias = logitBias,
        stream = stream,
        enableWebSearch = false,
        apiKey = apiKey,
        apiUrl = apiUrl,
        jsonSchema = jsonSchema,
        jsonSchemaName = jsonSchemaName,
        retryPolicy = retryPolicy,
        usageSink = usageSink,
        organization = organization,
        streamParsingMode = streamParsingMode,
        ndjsonDelimiter = ndjsonDelimiter,
    )
}
