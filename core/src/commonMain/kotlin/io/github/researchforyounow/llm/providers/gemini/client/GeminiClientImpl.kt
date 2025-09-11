package io.github.researchforyounow.llm.providers.gemini.client

import io.github.researchforyounow.llm.client.execute
import io.github.researchforyounow.llm.error.LlmError
import io.github.researchforyounow.llm.providers.gemini.config.GeminiConfig
import io.github.researchforyounow.llm.providers.gemini.request.ApiRequest
import io.github.researchforyounow.llm.providers.gemini.request.GeminiRequestBuilder
import io.github.researchforyounow.llm.providers.gemini.response.GeminiContentExtractor
import io.github.researchforyounow.llm.request.GenerationRequest
import io.github.researchforyounow.llm.response.JsonResponseProcessor
import io.github.researchforyounow.llm.response.StreamChunk
import io.github.researchforyounow.llm.response.TypedStreamChunk
import io.github.researchforyounow.llm.usage.LlmUsage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/**
 * Implementation of the GeminiClient interface.
 * This class handles communication with the Gemini API.
 */
class GeminiClientImpl private constructor(
    private val httpClient: HttpClient,
    private val config: GeminiConfig,
    private val requestBuilder: GeminiRequestBuilder,
) : GeminiClient {
    private val logger = LoggerFactory.getLogger(GeminiClientImpl::class.java)

    // Configure JSON parser with lenient mode to handle potential JSON issues
    private val jsonParser =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    /**
     * Generates content using the Gemini API.
     * This simplified method uses a configuration object to encapsulate request parameters,
     * making the API cleaner and more maintainable.
     *
     * Generation parameters (temperature, topK, topP, maxOutputTokens, etc.) are configured through
     * the GeminiConfig that was provided when creating this client instance.
     *
     * @param request The generation request configuration containing prompt and optional system message
     * @param responseType The class of the expected response type
     * @return A Result containing the parsed response or an error
     */
    override suspend fun <T> generate(
        request: GenerationRequest,
        responseType: Class<T>,
    ): Result<T> {
        return try {
            val apiRequest = when {
                !request.systemMessage.isNullOrEmpty() ->
                    requestBuilder.buildRequest(
                        systemMessage = request.systemMessage ?: "",
                        prompt = request.prompt,
                    )

                else -> requestBuilder.buildRequest(
                    instructions = request.prompt,
                )
            }

            // Execute the request
            val response = executeRequest(apiRequest, request)

            // If plain text is requested, extract directly without JSON deserialization
            if (responseType == String::class.java) {
                val contentResult =
                    GeminiContentExtractor.extractContent(
                        responseJson = response,
                        cleanMarkdown = config.cleanMarkdownCodeBlocks,
                    )
                @Suppress("UNCHECKED_CAST")
                return contentResult.map { it as T }
            }

            // Otherwise process as structured JSON
            JsonResponseProcessor.processResponse(
                responseJson = response,
                targetClass = responseType,
                contentExtractor = { json ->
                    GeminiContentExtractor.extractContent(
                        responseJson = json,
                        cleanMarkdown = config.cleanMarkdownCodeBlocks,
                    )
                },
                jsonParser = jsonParser,
                logger = logger,
            )
        } catch (e: Exception) {
            logger.error("Error generating content from Gemini API", e)
            Result.failure(LlmError.fromException(e))
        }
    }

    override suspend fun generateText(
        request: GenerationRequest,
    ): Result<String> {
        return generate(request, String::class.java)
    }

    /**
     * Returns the model name being used by this client.
     */
    override fun getModelName(): String {
        return config.model.modelName
    }

    /**
     * Returns the API key being used by this client.
     */
    override fun getApiKey(): String {
        return config.apiKey
    }

    override fun stream(
        request: GenerationRequest,
    ): Flow<StreamChunk> {
        return flow {
            val result = generateText(request)
            result.onSuccess { emit(StreamChunk(it)) }
                .onFailure { throw it }
        }
    }

    override fun <T> streamTyped(
        request: GenerationRequest,
        responseType: Class<T>,
    ): Flow<TypedStreamChunk<T>> {
        return flow {
            val result = generate(request, responseType)
            result.onSuccess { parsedContent ->
                // For Gemini, we emit the complete parsed result as a single chunk
                // since the current implementation doesn't support true streaming
                val rawContent = "Generated content" // We don't have access to raw content in this approach
                emit(TypedStreamChunk(parsedContent, rawContent))
            }.onFailure { error ->
                emit(TypedStreamChunk<T>(null, "", "Error generating content: ${error.message}"))
            }
        }
    }

    /**
     * Executes the request and returns the raw response.
     * This is an internal implementation detail.
     */
    private suspend fun executeRequest(
        request: ApiRequest,
        genRequest: GenerationRequest,
    ): JsonObject {
        logger.info("Sending request to Gemini API")

        try {
            val call = suspend {
                val response = httpClient.post {
                    url(config.apiUrl)
                    contentType(ContentType.Application.Json)
                    url {
                        parameters.append("key", config.apiKey)
                    }
                    genRequest.tags?.takeIf { it.isNotEmpty() }?.let { tagsMap ->
                        val sanitized =
                            tagsMap.entries.joinToString(";") { (k, v) ->
                                val sk = k.replace(";", "_").replace("=", ":")
                                val sv = v.replace(";", "_").replace("=", ":")
                                "$sk=$sv"
                            }
                        headers.append("X-Request-Tags", sanitized)
                    }
                    setBody(request)
                }
                if (!response.status.isSuccess()) {
                    val status = response.status.value
                    val errorBody = response.bodyAsText()
                    logger.error("Gemini API error response: {} - {}", response.status, errorBody)
                    if (status == 429) {
                        val retryAfterHeader = response.headers["Retry-After"]
                        val retryAfter = parseRetryAfterSeconds(retryAfterHeader)
                        throw LlmError.RateLimitError(
                            retryAfterSeconds = retryAfter,
                            message = "Rate limited by Gemini",
                        )
                    }
                    throw LlmError.ProviderHttpError(status, errorBody)
                }
                response.body<JsonObject>()
            }
            val responseJson =
                if (genRequest.idempotencyKey?.isNotBlank() == true) {
                    config.retryPolicy.execute { call() }
                } else {
                    call()
                }
            logger.info("Received response from Gemini API")

            try {
                val usageMeta = responseJson["usageMetadata"] as? JsonObject
                val prompt = usageMeta?.get("promptTokenCount")?.jsonPrimitive?.intOrNull
                val completion = usageMeta?.get("candidatesTokenCount")?.jsonPrimitive?.intOrNull
                val total = usageMeta?.get("totalTokenCount")?.jsonPrimitive?.intOrNull
                if (prompt != null && completion != null) {
                    config.usageSink?.invoke(
                        LlmUsage(promptTokens = prompt, completionTokens = completion, totalTokens = total),
                    )
                }
            } catch (_: Exception) {
                // ignore usage parsing errors
            }

            return responseJson
        } catch (e: Exception) {
            logger.error("Error calling Gemini API", e)
            throw e
        }
    }

    /**
     * Builder class for GeminiClientImpl.
     */
    class Builder : GeminiClient.Builder {
        private var httpClient: HttpClient? = null
        private var config: GeminiConfig? = null

        /**
         * Sets the HTTP client.
         */
        override fun httpClient(
            httpClient: HttpClient,
        ): GeminiClient.Builder = apply { this.httpClient = httpClient }

        /**
         * Sets the configuration.
         */
        override fun config(
            config: GeminiConfig,
        ): GeminiClient.Builder = apply { this.config = config }

        /**
         * Builds the GeminiClientImpl instance.
         * @throws IllegalStateException if any required parameter is missing
         */
        override fun build(): GeminiClient {
            val finalHttpClient = httpClient ?: throw IllegalStateException(
                "HttpClient is required; pass your own instance via builder.httpClient(...)",
            )
            val finalConfig = config ?: throw IllegalStateException("Configuration is required")

            val requestBuilder = GeminiRequestBuilder.create(finalConfig)

            return GeminiClientImpl(
                httpClient = finalHttpClient,
                config = finalConfig,
                requestBuilder = requestBuilder,
            )
        }
    }

    companion object {
        /**
         * Creates a new builder for GeminiClientImpl.
         */
        fun builder(): Builder = Builder()
    }
}

// Parses Retry-After header value into seconds. Accepts either integer seconds or RFC1123 date.
private fun parseRetryAfterSeconds(
    header: String?,
): Long? {
    if (header.isNullOrBlank()) return null
    val trimmed = header.trim()
    trimmed.toLongOrNull()?.let { if (it >= 0) return it }
    return try {
        val instant = java.time.ZonedDateTime.parse(
            trimmed,
            java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME,
        ).toInstant()
        val now = java.time.Instant.now()
        val seconds = java.time.Duration.between(now, instant).seconds
        if (seconds > 0) seconds else null
    } catch (_: Exception) {
        null
    }
}
