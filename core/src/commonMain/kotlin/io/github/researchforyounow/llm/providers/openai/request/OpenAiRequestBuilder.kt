package io.github.researchforyounow.llm.providers.openai.request

import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.config.ResponseFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Request builder for OpenAI API.
 * This class provides methods for building requests for the OpenAI API.
 *
 * This class is internal and should not be used directly by consumers.
 */
internal class OpenAiRequestBuilder private constructor(
    private val config: OpenAiConfig,
) {
    /**
     * Builds a request for the OpenAI API with default parameters from the configuration.
     *
     * @param systemMessage The system message that provides context and instructions
     * @param prompt The user prompt or request
     * @return A JsonObject containing the OpenAI API request
     */
    fun buildRequest(
        systemMessage: String,
        prompt: String,
    ): JsonObject {
        return buildRequest(
            systemMessage = systemMessage,
            prompt = prompt,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
        )
    }

    /**
     * Builds a streaming request for the OpenAI API with default parameters
     * and enables server-side streaming.
     */
    fun buildStreamRequest(
        systemMessage: String,
        prompt: String,
    ): JsonObject {
        return buildRequestWithAllParams(
            systemMessage = systemMessage,
            prompt = prompt,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            topP = config.topP,
            frequencyPenalty = config.frequencyPenalty,
            presencePenalty = config.presencePenalty,
            stopSequences = config.stopSequences,
            seed = config.seed,
            responseFormat = config.responseFormat,
            user = config.user,
            logitBias = config.logitBias,
            stream = true,
        )
    }

    /**
     * Builds a request for the OpenAI API with custom parameters.
     *
     * @param systemMessage The system message that provides context and instructions
     * @param prompt The user prompt or request
     * @param temperature The sampling temperature (0.0 to 2.0)
     * @param maxTokens The maximum number of tokens to generate
     * @return A JsonObject containing the OpenAI API request
     */
    fun buildRequest(
        systemMessage: String,
        prompt: String,
        temperature: Double,
        maxTokens: Int,
    ): JsonObject {
        return buildRequestWithAllParams(
            systemMessage = systemMessage,
            prompt = prompt,
            temperature = temperature,
            maxTokens = maxTokens,
            topP = config.topP,
            frequencyPenalty = config.frequencyPenalty,
            presencePenalty = config.presencePenalty,
            stopSequences = config.stopSequences,
            seed = config.seed,
            responseFormat = config.responseFormat,
            user = config.user,
            logitBias = config.logitBias,
            stream = config.stream,
        )
    }

    /**
     * Builds a request for the OpenAI API with all configuration parameters.
     *
     * @param systemMessage The system message that provides context and instructions
     * @param prompt The user prompt or request
     * @param temperature The sampling temperature (0.0 to 2.0)
     * @param maxTokens The maximum number of tokens to generate
     * @param topP The nucleus sampling parameter (0.0 to 1.0)
     * @param frequencyPenalty The frequency penalty (-2.0 to 2.0)
     * @param presencePenalty The presence penalty (-2.0 to 2.0)
     * @param stopSequences The stop sequences that will halt generation
     * @param seed The seed for deterministic outputs
     * @param responseFormat The response format
     * @param user The user identifier
     * @param logitBias The logit bias
     * @param stream Whether to stream back partial progress
     * @return A JsonObject containing the OpenAI API request
     */
    private fun buildRequestWithAllParams(
        systemMessage: String,
        prompt: String,
        temperature: Double,
        maxTokens: Int,
        topP: Double,
        frequencyPenalty: Double,
        presencePenalty: Double,
        stopSequences: List<String>,
        seed: Int?,
        responseFormat: ResponseFormat,
        user: String?,
        logitBias: Map<String, Double>,
        stream: Boolean,
    ): JsonObject {
        // Check if either message contains the word "json" when using json_object response format
        var finalPrompt = prompt
        if (responseFormat == ResponseFormat.JSON_OBJECT &&
            !systemMessage.contains("json", ignoreCase = true) &&
            !prompt.contains("json", ignoreCase = true)
        ) {
            // Append request for JSON format to the prompt
            finalPrompt = "$prompt Please provide your response in JSON format."
        }

        return buildJsonObject {
            put("model", config.model.modelName)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemMessage)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", finalPrompt)
                }
            }
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("top_p", topP)
            put("frequency_penalty", frequencyPenalty)
            put("presence_penalty", presencePenalty)
            put("stream", stream)

            // Add stop sequences if provided
            if (stopSequences.isNotEmpty()) {
                putJsonArray("stop") {
                    stopSequences.forEach { add(JsonPrimitive(it)) }
                }
            }

            // Add seed if provided
            seed?.let { put("seed", it) }

            // Add user if provided
            user?.let { put("user", it) }

            // Add logit bias if provided
            if (logitBias.isNotEmpty()) {
                buildJsonObject {
                    logitBias.forEach { (token, bias) ->
                        put(token, bias)
                    }
                }.also { logitBiasObject ->
                    put("logit_bias", logitBiasObject)
                }
            }

            // Add response format
            when (responseFormat) {
                ResponseFormat.JSON_OBJECT -> {
                    buildJsonObject {
                        put("type", "json_object")
                    }.also { responseFormatObject ->
                        put("response_format", responseFormatObject)
                    }
                }
                ResponseFormat.TEXT -> {
                    buildJsonObject {
                        put("type", "text")
                    }.also { responseFormatObject ->
                        put("response_format", responseFormatObject)
                    }
                }
                ResponseFormat.JSON_SCHEMA -> {
                    require(!config.jsonSchema.isNullOrBlank()) {
                        "jsonSchema must be provided when responseFormat == JSON_SCHEMA"
                    }
                    val schemaElement = Json.parseToJsonElement(config.jsonSchema)
                    buildJsonObject {
                        put("type", "json_schema")
                        put("json_schema", schemaElement)
                    }.also { responseFormatObject ->
                        put("response_format", responseFormatObject)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Creates a new OpenAiRequestBuilder with the given configuration.
         */
        fun create(
            config: OpenAiConfig,
        ): OpenAiRequestBuilder {
            return OpenAiRequestBuilder(config)
        }
    }
}
