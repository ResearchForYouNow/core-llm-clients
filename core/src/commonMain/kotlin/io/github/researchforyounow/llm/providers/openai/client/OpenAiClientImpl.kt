package io.github.researchforyounow.llm.providers.openai.client

import io.github.researchforyounow.llm.client.execute
import io.github.researchforyounow.llm.error.LlmError
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.config.StreamParsingMode
import io.github.researchforyounow.llm.providers.openai.request.AudioMultipartBuilder
import io.github.researchforyounow.llm.providers.openai.request.AudioResponseFormat
import io.github.researchforyounow.llm.providers.openai.request.AudioTranscriptionRequest
import io.github.researchforyounow.llm.providers.openai.request.AudioTranslationRequest
import io.github.researchforyounow.llm.providers.openai.request.ImageGenerationRequest
import io.github.researchforyounow.llm.providers.openai.request.ImageResponseFormat
import io.github.researchforyounow.llm.providers.openai.request.OpenAiImageModel
import io.github.researchforyounow.llm.providers.openai.request.OpenAiRequestBuilder
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeNoiseReduction
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionConnection
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionSessionRequest
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionSessionResponse
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTurnDetection
import io.github.researchforyounow.llm.providers.openai.response.AudioTranscriptionResponse
import io.github.researchforyounow.llm.providers.openai.response.AudioTranscriptionStreamEvent
import io.github.researchforyounow.llm.providers.openai.response.ImageResult
import io.github.researchforyounow.llm.providers.openai.response.OpenAiContentExtractor
import io.github.researchforyounow.llm.request.GenerationRequest
import io.github.researchforyounow.llm.response.JsonResponseProcessor
import io.github.researchforyounow.llm.response.StreamChunk
import io.github.researchforyounow.llm.response.TypedStreamChunk
import io.github.researchforyounow.llm.usage.LlmUsage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import org.slf4j.LoggerFactory
import io.ktor.websocket.Frame
import kotlin.coroutines.cancellation.CancellationException

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
        return config.modelName
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

    override suspend fun transcribe(
        request: AudioTranscriptionRequest,
    ): Result<AudioTranscriptionResponse> {
        if (request.stream) {
            return Result.failure(
                LlmError.InvalidRequestError("Use streamTranscription(...) for streaming audio requests"),
            )
        }

        return try {
            logger.info("Creating transcription with OpenAI Audio API")
            val call = suspend {
                httpClient.post(audioApiUrl("transcriptions")) {
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
                    setBody(AudioMultipartBuilder.buildTranscriptionContent(request, stream = false))
                }
            }

            val response = when {
                request.idempotencyKey?.isNotBlank() == true -> config.retryPolicy.execute { call() }
                else -> call()
            }

            if (!response.status.isSuccess()) {
                return Result.failure(
                    handleAudioError(
                        status = response.status.value,
                        errorBody = response.bodyAsText(),
                        response = response
                    )
                )
            }

            val bodyText = response.bodyAsText()
            Result.success(parseAudioResponse(request.responseFormat, bodyText))
        } catch (e: Exception) {
            logger.error("Error creating transcription from OpenAI Audio API", e)
            Result.failure(LlmError.fromException(e))
        }
    }

    override fun streamTranscription(
        request: AudioTranscriptionRequest,
    ): Flow<AudioTranscriptionStreamEvent> {
        return flow {
            val apiRequest = request.copy(stream = true)
            val sse = SseLineAccumulator()

            httpClient.preparePost(audioApiUrl("transcriptions")) {
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
                setBody(AudioMultipartBuilder.buildTranscriptionContent(apiRequest, stream = true))
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    val status = response.status.value
                    val errorBody = response.bodyAsText()
                    throw handleAudioError(status, errorBody, response)
                }

                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    val payload = sse.onLine(line) ?: continue
                    if (payload == "[DONE]") break
                    try {
                        val json = jsonParser.parseToJsonElement(payload).jsonObject
                        val type = json["type"]?.jsonPrimitive?.contentOrNull
                        emit(AudioTranscriptionStreamEvent(type = type, payload = json))
                    } catch (e: Exception) {
                        logger.error("Error parsing transcription stream event", e)
                    }
                }
            }
        }
    }

    override suspend fun translate(
        request: AudioTranslationRequest,
    ): Result<AudioTranscriptionResponse> {
        return try {
            logger.info("Creating translation with OpenAI Audio API")
            val call = suspend {
                httpClient.post(audioApiUrl("translations")) {
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
                    setBody(AudioMultipartBuilder.buildTranslationContent(request))
                }
            }

            val response = when {
                request.idempotencyKey?.isNotBlank() == true -> config.retryPolicy.execute { call() }
                else -> call()
            }

            if (!response.status.isSuccess()) {
                return Result.failure(
                    handleAudioError(
                        status = response.status.value,
                        errorBody = response.bodyAsText(),
                        response = response
                    )
                )
            }

            val bodyText = response.bodyAsText()
            Result.success(parseAudioResponse(request.responseFormat, bodyText))
        } catch (e: Exception) {
            logger.error("Error creating translation from OpenAI Audio API", e)
            Result.failure(LlmError.fromException(e))
        }
    }

    override suspend fun createRealtimeTranscriptionSession(
        request: RealtimeTranscriptionSessionRequest,
    ): Result<RealtimeTranscriptionSessionResponse> {
        return try {
            logger.info("Creating realtime transcription session with OpenAI")
            val body = buildRealtimeSessionBody(request)
            val response = httpClient.post(realtimeApiUrl("transcription_sessions")) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${config.apiKey}")
                header("OpenAI-Beta", "realtime=v1")
                config.organization?.takeIf { it.isNotBlank() }?.let { header("OpenAI-Organization", it) }
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(
                    handleAudioError(
                        status = response.status.value,
                        errorBody = response.bodyAsText(),
                        response = response
                    )
                )
            }
            val text = response.bodyAsText()
            val parsed = jsonParser.decodeFromString(
                RealtimeTranscriptionSessionResponse.serializer(),
                text,
            )
            Result.success(parsed)
        } catch (e: Exception) {
            logger.error("Error creating realtime transcription session", e)
            Result.failure(LlmError.fromException(e))
        }
    }

    override suspend fun openRealtimeTranscriptionConnection(
        request: RealtimeTranscriptionSessionRequest,
        authToken: String?,
    ): Result<RealtimeTranscriptionConnection> {
        return try {
            val token = authToken ?: config.apiKey
            val session = httpClient.webSocketSession {
                url(realtimeWsUrl("transcription"))
                header("Authorization", "Bearer $token")
                header("OpenAI-Beta", "realtime=v1")
                config.organization?.takeIf { it.isNotBlank() }?.let { header("OpenAI-Organization", it) }
            }

            val updateEvent = buildRealtimeSessionUpdateEvent(request)
            session.send(Frame.Text(jsonParser.encodeToString(JsonObject.serializer(), updateEvent)))

            Result.success(RealtimeTranscriptionConnection(session = session, json = jsonParser))
        } catch (e: Exception) {
            logger.error("Error opening realtime transcription connection", e)
            Result.failure(LlmError.fromException(e))
        }
    }

    override suspend fun openRealtimeTranscriptionConnection(
        request: RealtimeTranscriptionSessionRequest,
        session: RealtimeTranscriptionSessionResponse,
    ): Result<RealtimeTranscriptionConnection> {
        val token = session.clientSecret?.value ?: config.apiKey
        return try {
            val wsSession = httpClient.webSocketSession {
                url(realtimeWsUrl("transcription"))
                header("Authorization", "Bearer $token")
                header("OpenAI-Beta", "realtime=v1")
                config.organization?.takeIf { it.isNotBlank() }?.let { header("OpenAI-Organization", it) }
            }

            Result.success(
                RealtimeTranscriptionConnection(
                    session = wsSession,
                    json = jsonParser,
                    sessionId = session.id,
                ),
            )
        } catch (e: Exception) {
            logger.error("Error opening realtime transcription connection", e)
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
                require(
                    req.size in allowed,
                ) { "Invalid size for dall-e-3: ${req.size}. Allowed: ${allowed.joinToString()}" }
                req.quality?.let {
                    require(
                        it == "standard" || it == "hd",
                    ) { "Invalid quality for dall-e-3: $it. Allowed: standard, hd" }
                }
            }

            OpenAiImageModel.GPT_IMAGE_1 -> {
                val allowed = setOf("256x256", "512x512", "1024x1024")
                require(
                    req.size in allowed,
                ) { "Invalid size for gpt-image-1: ${req.size}. Allowed: ${allowed.joinToString()}" }
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

    private fun audioApiUrl(
        path: String,
    ): String {
        val marker = "/v1/"
        val idx = config.apiUrl.indexOf(marker)
        return when {
            idx > 0 -> config.apiUrl.substring(0, idx + marker.length) + "audio/$path"
            else -> "https://api.openai.com/v1/audio/$path"
        }
    }

    private fun realtimeApiUrl(
        path: String,
    ): String {
        val marker = "/v1/"
        val idx = config.apiUrl.indexOf(marker)
        return when {
            idx > 0 -> config.apiUrl.take(idx + marker.length) + "realtime/$path"
            else -> "https://api.openai.com/v1/realtime/$path"
        }
    }

    private fun realtimeWsUrl(
        intent: String,
    ): String {
        val httpBase = realtimeApiUrl("").removeSuffix("/")
        val wsBase = when {
            httpBase.startsWith("https://") -> "wss://" + httpBase.removePrefix("https://")
            httpBase.startsWith("http://") -> "ws://" + httpBase.removePrefix("http://")
            else -> httpBase
        }
        return "$wsBase?intent=$intent"
    }

    private fun buildRealtimeSessionBody(
        request: RealtimeTranscriptionSessionRequest,
    ): JsonObject {
        return buildJsonObject {
            put("input_audio_format", request.inputAudioFormat)
            put(
                "input_audio_transcription",
                buildJsonObject {
                    put("model", request.transcriptionModel)
                    request.prompt?.let { put("prompt", it) }
                    request.language?.let { put("language", it) }
                },
            )
            request.turnDetection?.let { put("turn_detection", buildTurnDetection(it)) }
            request.noiseReduction?.let { put("input_audio_noise_reduction", buildNoiseReduction(it)) }
            request.include?.let { includes ->
                put(
                    "include",
                    buildJsonArray {
                        includes.forEach { add(it) }
                    },
                )
            }
        }
    }

    private fun buildRealtimeSessionUpdateEvent(
        request: RealtimeTranscriptionSessionRequest,
    ): JsonObject {
        return buildJsonObject {
            put("type", "transcription_session.update")
            put("input_audio_format", request.inputAudioFormat)
            put(
                "input_audio_transcription",
                buildJsonObject {
                    put("model", request.transcriptionModel)
                    request.prompt?.let { put("prompt", it) }
                    request.language?.let { put("language", it) }
                },
            )
            request.turnDetection?.let { put("turn_detection", buildTurnDetection(it)) }
            request.noiseReduction?.let { put("input_audio_noise_reduction", buildNoiseReduction(it)) }
            request.include?.let { includes ->
                put(
                    "include",
                    buildJsonArray {
                        includes.forEach { add(it) }
                    },
                )
            }
        }
    }

    private fun buildTurnDetection(
        turnDetection: RealtimeTurnDetection,
    ): JsonObject {
        return buildJsonObject {
            put("type", turnDetection.type)
            turnDetection.threshold?.let { put("threshold", it) }
            turnDetection.prefixPaddingMs?.let { put("prefix_padding_ms", it) }
            turnDetection.silenceDurationMs?.let { put("silence_duration_ms", it) }
        }
    }

    private fun buildNoiseReduction(
        noiseReduction: RealtimeNoiseReduction,
    ): JsonObject {
        return buildJsonObject {
            put("type", noiseReduction.type)
        }
    }

    private fun handleAudioError(
        status: Int,
        errorBody: String,
        response: io.ktor.client.statement.HttpResponse,
    ): LlmError {
        logger.error("OpenAI Audio API error response: {} - {}", status, errorBody)
        if (status == 429) {
            val retryAfterHeader = response.headers["Retry-After"]
            val retryAfter = parseRetryAfterSeconds(retryAfterHeader)
            return LlmError.RateLimitError(retryAfterSeconds = retryAfter, message = "Rate limited by OpenAI")
        }
        return LlmError.ProviderHttpError(status, errorBody)
    }

    private fun parseAudioResponse(
        format: AudioResponseFormat,
        bodyText: String,
    ): AudioTranscriptionResponse {
        return when (format) {
            AudioResponseFormat.TEXT,
            AudioResponseFormat.SRT,
            AudioResponseFormat.VTT -> AudioTranscriptionResponse(text = bodyText)
            AudioResponseFormat.JSON,
            AudioResponseFormat.VERBOSE_JSON,
            AudioResponseFormat.DIARIZED_JSON -> jsonParser.decodeFromString(
                deserializer = AudioTranscriptionResponse.serializer(),
                string = bodyText,
            )
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

    override fun <T> streamTyped(
        request: GenerationRequest,
        responseType: Class<T>,
    ): Flow<TypedStreamChunk<T>> =
        flow {
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

                when (config.streamParsingMode) {
                    /** Parse one complete JSON object per line (NDJSON). */
                    StreamParsingMode.NDJSON_PER_LINE -> {
                        // inside StreamParsingMode.NDJSON_PER_LINE branch
                        val buf = StringBuilder()
                        val delim = config.ndjsonDelimiter

                        while (!channel.isClosedForRead) {
                            // bail out ASAP if downstream cancelled (e.g., take(10))
                            if (!currentCoroutineContext().isActive) {
                                try {
                                    response.cancel()
                                } catch (_: Throwable) {
                                }
                                return@execute
                            }

                            val line = channel.readUTF8Line() ?: break
                            if (!line.startsWith("data:")) continue

                            val data = line.removePrefix("data:").trim()
                            if (data == "[DONE]") break

                            val json = jsonParser.parseToJsonElement(data).jsonObject
                            val content = json["choices"]
                                ?.jsonArray?.firstOrNull()
                                ?.jsonObject?.get("delta")
                                ?.jsonObject?.get("content")
                                ?.jsonPrimitive?.contentOrNull

                            if (!content.isNullOrEmpty()) {
                                buf.append(content)

                                while (true) {
                                    val idx = buf.indexOf(delim)
                                    if (idx == -1) break

                                    val lineJson = buf.substring(0, idx).trim()
                                    buf.delete(0, idx + delim.length)
                                    if (lineJson.isEmpty()) continue

                                    // stop if cancelled before parsing/emitting
                                    if (!currentCoroutineContext().isActive) {
                                        try {
                                            response.cancel()
                                        } catch (_: Throwable) {
                                        }
                                        return@execute
                                    }

                                    try {
                                        val serializer = jsonParser.serializersModule.serializer(responseType)

                                        @Suppress("UNCHECKED_CAST")
                                        val obj = jsonParser.decodeFromString(serializer, lineJson) as T
                                        try {
                                            emit(TypedStreamChunk(obj, lineJson, null))
                                        } catch (ce: CancellationException) {
                                            try {
                                                response.cancel()
                                            } catch (_: Throwable) {
                                            }
                                            return@execute
                                        }
                                    } catch (ce: CancellationException) {
                                        // IMPORTANT: never swallow cancellation; do not emit from here
                                        try {
                                            response.cancel()
                                        } catch (_: Throwable) {
                                        }
                                        throw ce
                                    } catch (e: Exception) {
                                        // only emit error if still active
                                        if (!currentCoroutineContext().isActive) {
                                            try {
                                                response.cancel()
                                            } catch (_: Throwable) {
                                            }
                                            return@execute
                                        }
                                        try {
                                            emit(TypedStreamChunk<T>(null, lineJson, e.message))
                                        } catch (ce: CancellationException) {
                                            try {
                                                response.cancel()
                                            } catch (_: Throwable) {
                                            }
                                            return@execute
                                        }
                                    }
                                }
                            }
                        }

// Final leftover parse ONLY if still active
                        if (currentCoroutineContext().isActive) {
                            val leftover = buf.toString().trim()
                            if (leftover.isNotEmpty()) {
                                try {
                                    val serializer = jsonParser.serializersModule.serializer(responseType)

                                    @Suppress("UNCHECKED_CAST")
                                    val obj = jsonParser.decodeFromString(serializer, leftover) as T
                                    emit(TypedStreamChunk(obj, leftover, null))
                                } catch (ce: CancellationException) {
                                    throw ce
                                } catch (e: Exception) {
                                    emit(TypedStreamChunk<T>(null, leftover, e.message))
                                }
                            }
                        }
                    }

                    /** Accumulate full text and parse once at the end ([DONE]). */
                    StreamParsingMode.BUFFER_AND_PARSE_FINAL -> {
                        val buf = StringBuilder()

                        while (!channel.isClosedForRead) {
                            val line = channel.readUTF8Line() ?: break
                            if (!line.startsWith("data:")) continue

                            val data = line.removePrefix("data:").trim()
                            if (data == "[DONE]") break

                            val json = jsonParser.parseToJsonElement(data).jsonObject
                            val content = json["choices"]
                                ?.jsonArray?.firstOrNull()
                                ?.jsonObject?.get("delta")
                                ?.jsonObject?.get("content")
                                ?.jsonPrimitive?.contentOrNull

                            if (!content.isNullOrEmpty()) {
                                buf.append(content)
                                // You may also emit raw as progress:
                                emit(TypedStreamChunk<T>(null, content, null))
                            }
                        }

                        val full = buf.toString().trim()
                        if (full.isNotEmpty()) {
                            try {
                                val serializer = jsonParser.serializersModule.serializer(responseType)

                                @Suppress("UNCHECKED_CAST")
                                val parsed = jsonParser.decodeFromString(serializer, full) as T
                                emit(TypedStreamChunk(parsed, full, null))
                            } catch (e: Exception) {
                                emit(TypedStreamChunk<T>(null, full, "Final parse failed: ${e.message}"))
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
