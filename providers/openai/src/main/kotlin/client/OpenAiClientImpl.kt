package client

import config.OpenAiConfig
import error.LlmError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import request.GenerationRequest
import request.ImageGenerationRequest
import request.ImageResponseFormat
import request.OpenAiImageModel
import request.OpenAiRequestBuilder
import response.ImageResult
import response.JsonResponseProcessor
import response.OpenAiContentExtractor
import response.StreamChunk
import usage.LlmUsage

/**
 * Implementation of the OpenAiClient interface.
 * This class handles communication with the OpenAI API.
 */
class OpenAiClientImpl private constructor(
    private val httpClient: HttpClient,
    private val config: OpenAiConfig,
    private val requestBuilder: OpenAiRequestBuilder,
) : OpenAiClient {
    private val logger = LoggerFactory.getLogger(OpenAiClientImpl::class.java)

    // Configure JSON parser with lenient mode to handle potential JSON issues
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Generates content using the OpenAI API.
     * This simplified method uses a configuration object to encapsulate request parameters,
     * making the API cleaner and more maintainable.
     *
     * Generation parameters (temperature, maxTokens, etc.) are configured through
     * the OpenAiConfig that was provided when creating this client instance.
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
            // Log appropriate message
            if (!request.systemMessage.isNullOrEmpty()) {
                logger.info("Generating content with OpenAI API (with system message)")
            } else {
                logger.info("Generating content with OpenAI API")
            }

            // Create the request using client configuration
            val apiRequest = requestBuilder.buildRequest(
                systemMessage = request.systemMessage ?: "",
                prompt = request.prompt,
            )

            // Execute the request
            val response = executeRequest(apiRequest, request)

            // If caller wants plain text, don’t send it through the JSON processor
            if (responseType == String::class.java) {
                val contentResult = OpenAiContentExtractor.extractContent(response)
                @Suppress("UNCHECKED_CAST")
                return contentResult.map { it as T }
            }

            // Otherwise keep existing behavior for JSON outputs
            JsonResponseProcessor.processResponse(
                responseJson = response,
                targetClass = responseType,
                contentExtractor = OpenAiContentExtractor::extractContent,
                jsonParser = jsonParser,
                logger = logger,
            )
        } catch (e: Exception) {
            logger.error("Error generating content from OpenAI API", e)
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

    override suspend fun generateImage(
        request: ImageGenerationRequest,
    ): Result<List<ImageResult>> {
        return try {
            logger.info("Generating image(s) with OpenAI Images API")

            // Validate request against model-specific constraints to fail fast
            try {
                validateImageRequest(request)
            } catch (e: IllegalArgumentException) {
                return Result.failure(LlmError.InvalidRequestError(e.message ?: "Invalid request"))
            }

            val body = buildJsonObject {
                put("prompt", request.prompt)
                put("n", request.n)
                put("size", request.size)
                request.quality?.let { put("quality", it) }
                val fmt = when (request.responseFormat) {
                    ImageResponseFormat.URL -> "url"
                    ImageResponseFormat.B64_JSON -> "b64_json"
                }
                put("response_format", fmt)
                put("model", (request.model ?: OpenAiImageModel.GPT_IMAGE_1).modelName)
                request.user?.let { put("user", it) }
            }

            val call = suspend {
                val response = httpClient.post(imagesApiUrl()) {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${config.apiKey}")
                    config.organization?.takeIf { it.isNotBlank() }?.let { header("OpenAI-Organization", it) }
                    request.idempotencyKey?.takeIf { it.isNotBlank() }?.let { header("Idempotency-Key", it) }
                    request.tags?.takeIf { it.isNotEmpty() }?.let { tagsMap ->
                        val sanitized = tagsMap.entries.joinToString(";") { (k, v) ->
                            val sk = k.replace(";", "_").replace("=", ":")
                            val sv = v.replace(";", "_").replace("=", ":")
                            "$sk=$sv"
                        }
                        header("X-Request-Tags", sanitized)
                    }
                    setBody(body)
                }
                // If we are using idempotency, allow automatic retries for transient server issues
                if (request.idempotencyKey?.isNotBlank() == true && !response.status.isSuccess()) {
                    val status = response.status.value
                    if (status == 408 || status in 500..504) {
                        val errorBody = response.bodyAsText()
                        val reqId = response.headers["x-request-id"] ?: response.headers["X-Request-Id"]
                        logger.warn(
                            "Transient Images API error (will retry): {} - {} (x-request-id={})",
                            response.status,
                            errorBody,
                            reqId ?: "n/a",
                        )
                        throw LlmError.ProviderHttpError(status, errorBody)
                    }
                }
                response
            }

            val response = if (request.idempotencyKey?.isNotBlank() == true) {
                config.retryPolicy.execute { call() }
            } else {
                call()
            }

            if (!response.status.isSuccess()) {
                val status = response.status.value
                val errorBody = response.bodyAsText()
                val reqId = response.headers["x-request-id"] ?: response.headers["X-Request-Id"]
                val shortMsg = extractProviderErrorMessage(errorBody)
                if (shortMsg != null) {
                    logger.error(
                        "OpenAI Images API error response: {} - {} (message={}) (x-request-id={})",
                        response.status,
                        errorBody,
                        shortMsg,
                        reqId ?: "n/a",
                    )
                } else {
                    logger.error(
                        "OpenAI Images API error response: {} - {} (x-request-id={})",
                        response.status,
                        errorBody,
                        reqId ?: "n/a",
                    )
                }
                if (status == 429) {
                    val retryAfterHeader = response.headers["Retry-After"]
                    val retryAfter = parseRetryAfterSeconds(retryAfterHeader)
                    return Result.failure(
                        LlmError.RateLimitError(retryAfterSeconds = retryAfter, message = "Rate limited by OpenAI"),
                    )
                }
                return Result.failure(LlmError.ProviderHttpError(status, errorBody))
            }

            val text = response.bodyAsText()
            val json = jsonParser.parseToJsonElement(text).jsonObject
            val dataArray = json["data"]?.jsonArray ?: return Result.success(emptyList())
            val images = dataArray.map { item ->
                val obj = item.jsonObject
                val url = obj["url"]?.jsonPrimitive?.content
                val b64 = obj["b64_json"]?.jsonPrimitive?.content
                ImageResult(url = url, b64Json = b64)
            }
            Result.success(images)
        } catch (e: Exception) {
            logger.error("Error generating images from OpenAI API", e)
            Result.failure(LlmError.fromException(e))
        }
    }

    private fun validateImageRequest(
        req: ImageGenerationRequest,
    ) {
        val model = req.model ?: OpenAiImageModel.GPT_IMAGE_1
        when (model) {
            OpenAiImageModel.DALL_E_3 -> {
                require(req.n == 1) { "dall-e-3 supports n=1 only, got: ${req.n}" }
                val allowed = setOf("1024x1024", "1024x1792", "1792x1024")
                require(req.size in allowed) { "Invalid size for dall-e-3: ${req.size}. Allowed: ${allowed.joinToString()}" }
                req.quality?.let {
                    require(it == "standard" || it == "hd") { "Invalid quality for dall-e-3: $it. Allowed: standard, hd" }
                }
            }
            OpenAiImageModel.GPT_IMAGE_1 -> {
                val allowed = setOf("256x256", "512x512", "1024x1024")
                require(req.size in allowed) { "Invalid size for gpt-image-1: ${req.size}. Allowed: ${allowed.joinToString()}" }
            }
        }
    }

    private fun extractProviderErrorMessage(
        body: String?,
    ): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val obj = jsonParser.parseToJsonElement(body).jsonObject
            val err = obj["error"]?.jsonObject
            val msg = err?.get("message")?.jsonPrimitive
            try {
                msg?.content
            } catch (_: Exception) {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun imagesApiUrl(): String {
        // derive base from config.apiUrl if possible
        val marker = "/v1/"
        val idx = config.apiUrl.indexOf(marker)
        return if (idx > 0) {
            config.apiUrl.substring(0, idx + marker.length) + "images/generations"
        } else {
            "https://api.openai.com/v1/images/generations"
        }
    }

    override fun stream(
        request: GenerationRequest,
    ): Flow<StreamChunk> {
        return flow {
            val apiRequest = requestBuilder.buildStreamRequest(
                systemMessage = request.systemMessage ?: "",
                prompt = request.prompt,
            )

            httpClient.preparePost(config.apiUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${config.apiKey}")
                config.organization?.takeIf { it.isNotBlank() }?.let { header("OpenAI-Organization", it) }
                request.idempotencyKey?.takeIf { it.isNotBlank() }?.let { header("Idempotency-Key", it) }
                request.tags?.takeIf { it.isNotEmpty() }?.let { tagsMap ->
                    val sanitized = tagsMap.entries.joinToString(";") { (k, v) ->
                        val sk = k.replace(";", "_").replace("=", ":")
                        val sv = v.replace(";", "_").replace("=", ":")
                        "$sk=$sv"
                    }
                    header("X-Request-Tags", sanitized)
                }
                setBody(apiRequest)
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val status = response.status.value
                    val errorBody = response.bodyAsText()
                    if (status == 429) {
                        val retryAfterHeader = response.headers["Retry-After"]
                        val retryAfter = parseRetryAfterSeconds(retryAfterHeader)
                        throw LlmError.RateLimitError(
                            retryAfterSeconds = retryAfter,
                            message = "Rate limited by OpenAI",
                        )
                    }
                    throw LlmError.ProviderHttpError(status, errorBody)
                }

                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        try {
                            val json = jsonParser.parseToJsonElement(data).jsonObject
                            val choices = json["choices"]
                            val delta = choices?.jsonArray?.firstOrNull()?.jsonObject?.get("delta")
                            val content = delta?.jsonObject?.get("content")?.toString()?.trim('"')
                            if (!content.isNullOrEmpty()) {
                                emit(StreamChunk(content))
                            }
                        } catch (e: Exception) {
                            logger.error("Error parsing stream chunk", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes the request and returns the raw response.
     * This is an internal implementation detail.
     */
    private suspend fun executeRequest(
        request: JsonObject,
        genRequest: GenerationRequest,
    ): JsonObject {
        logger.info("Sending request to OpenAI API")

        try {
            val call = suspend {
                httpClient.post(config.apiUrl) {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${config.apiKey}")
                    config.organization?.takeIf { it.isNotBlank() }?.let { header("OpenAI-Organization", it) }
                    genRequest.idempotencyKey?.takeIf { it.isNotBlank() }?.let { header("Idempotency-Key", it) }
                    genRequest.tags?.takeIf { it.isNotEmpty() }?.let { tagsMap ->
                        val sanitized = tagsMap.entries.joinToString(";") { (k, v) ->
                            val sk = k.replace(";", "_").replace("=", ":")
                            val sv = v.replace(";", "_").replace("=", ":")
                            "$sk=$sv"
                        }
                        header("X-Request-Tags", sanitized)
                    }
                    setBody(request)
                }
            }
            val response = if (genRequest.idempotencyKey?.isNotBlank() == true) {
                config.retryPolicy.execute { call() }
            } else {
                call()
            }

            // Check if the response is successful
            if (!response.status.isSuccess()) {
                val status = response.status.value
                val errorBody = response.bodyAsText()
                logger.error("OpenAI API error response: {} - {}", response.status, errorBody)
                if (status == 429) {
                    val retryAfterHeader = response.headers["Retry-After"]
                    val retryAfter = parseRetryAfterSeconds(retryAfterHeader)
                    throw LlmError.RateLimitError(retryAfterSeconds = retryAfter, message = "Rate limited by OpenAI")
                }
                throw LlmError.ProviderHttpError(status, errorBody)
            }

            val responseJson = response.body<JsonObject>()
            logger.info("Received response from OpenAI API")

            try {
                val usage = responseJson["usage"] as? JsonObject
                val prompt = usage?.get("prompt_tokens")?.jsonPrimitive?.intOrNull
                val completion = usage?.get("completion_tokens")?.jsonPrimitive?.intOrNull
                val total = usage?.get("total_tokens")?.jsonPrimitive?.intOrNull
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
            logger.error("Error calling OpenAI API", e)
            throw e
        }
    }

    /**
     * Builder class for OpenAiClientImpl.
     */
    class Builder : OpenAiClient.Builder {
        private var httpClient: HttpClient? = null
        private var config: OpenAiConfig? = null

        /**
         * Sets the HTTP client.
         */
        override fun httpClient(
            httpClient: HttpClient,
        ): OpenAiClient.Builder = apply { this.httpClient = httpClient }

        /**
         * Sets the configuration.
         */
        override fun config(
            config: OpenAiConfig,
        ): OpenAiClient.Builder = apply { this.config = config }

        /**
         * Builds the OpenAiClientImpl instance.
         * @throws IllegalStateException if any required parameter is missing
         */
        override fun build(): OpenAiClient {
            val finalHttpClient = httpClient ?: throw IllegalStateException(
                "HttpClient is required; pass your own instance via builder.httpClient(...)",
            )
            val finalConfig = config ?: throw IllegalStateException("Configuration is required")

            val requestBuilder = OpenAiRequestBuilder.create(finalConfig)

            return OpenAiClientImpl(
                httpClient = finalHttpClient,
                config = finalConfig,
                requestBuilder = requestBuilder,
            )
        }
    }

    companion object {
        /**
         * Creates a new builder for OpenAiClientImpl.
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
