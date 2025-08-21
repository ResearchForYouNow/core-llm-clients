package client

import config.GeminiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import request.GenerationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeminiClientContractTest {
    private fun jsonEscape(
        text: String,
    ): String {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }

    private fun successJson(
        text: String,
    ): String {
        return """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "${jsonEscape(text)}" }
                    ]
                  }
                }
              ],
              "usageMetadata": {"promptTokenCount": 1, "candidatesTokenCount": 2, "totalTokenCount": 3}
            }
            """.trimIndent()
    }

    @Test
    fun generateText_returnsExpectedText_andExposesConfig() =
        runBlocking {
            val engine = MockEngine { _ ->
                respond(
                    content = successJson("Hello world"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, listOf(ContentType.Application.Json.toString())),
                )
            }
            val http = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        },
                    )
                }
                install(HttpTimeout)
            }
            val config = GeminiConfig.defaultConfig().withApiKey("test-key")
            val client = GeminiClient.builder().httpClient(http).config(config).build()

            val req = GenerationRequest.of("Say hi")
            val result = client.generateText(req)

            assertTrue(result.isSuccess)
            assertEquals("Hello world", result.getOrThrow())
            assertEquals(config.model.modelName, client.getModelName())
            assertEquals("test-key", client.getApiKey())
        }

    @Test
    fun generateText_returnsRawFenced_whenCleanupDisabled() =
        runBlocking {
            val engine = MockEngine { _ ->
                respond(
                    content = successJson(
                        """```json
                              {"a":1}
                          ```""",
                    ),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, listOf(ContentType.Application.Json.toString())),
                )
            }
            val http = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        },
                    )
                }
                install(HttpTimeout)
            }
            val config = GeminiConfig.defaultConfig().copy(cleanMarkdownCodeBlocks = false).withApiKey("key")
            val client = GeminiClient.builder().httpClient(http).config(config).build()

            val result = client.generateText(GenerationRequest.of("prompt"))
            assertTrue(result.isSuccess, "Expected success, got $result")
            assertEquals("```json\n{\"a\":1}\n```", result.getOrThrow())
        }

    @Test
    fun stream_emitsSingleChunk_matchingGenerateText() =
        runBlocking {
            val engine = MockEngine { _ ->
                respond(
                    content = successJson("Hello stream"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, listOf(ContentType.Application.Json.toString())),
                )
            }
            val http = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        },
                    )
                }
                install(HttpTimeout)
            }
            val config = GeminiConfig.defaultConfig().withApiKey("key")
            val client = GeminiClient.builder().httpClient(http).config(config).build()

            val chunks = client.stream(GenerationRequest.of("Stream it")).toList()
            assertEquals(listOf("Hello stream"), chunks.map { it.content })
        }

    @Test
    fun retry_onFailure_thenSuccess_withIdempotencyKey() =
        runBlocking {
            var calls = 0
            val engine = MockEngine { _ ->
                calls++
                if (calls == 1) {
                    respond(
                        content = "{\"error\":{\"message\":\"temporary\"}}",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(
                            HttpHeaders.ContentType,
                            listOf(ContentType.Application.Json.toString()),
                        ),
                    )
                } else {
                    respond(
                        content = successJson("OK after retry"),
                        status = HttpStatusCode.OK,
                        headers = headersOf(
                            HttpHeaders.ContentType,
                            listOf(ContentType.Application.Json.toString()),
                        ),
                    )
                }
            }
            val http = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        },
                    )
                }
                install(HttpTimeout)
            }
            val retryingConfig = GeminiConfig.defaultConfig().copy(
                // withApiKey also sets apiUrl, but copy preserves url if already set
                apiKey = "key",
                retryPolicy = client.RetryPolicy(
                    maxAttempts = 2,
                    initialDelayMillis = 0,
                    maxDelayMillis = 0,
                    jitterMillis = 0,
                ),
            )
            val client = GeminiClient.builder().httpClient(http).config(retryingConfig).build()

            val req = GenerationRequest.builder().prompt("Try it").idempotencyKey("abc").build()

            val result = client.generateText(req)
            assertTrue(result.isSuccess, "Expected success after retry, got $result")
            assertEquals("OK after retry", result.getOrThrow())
            assertEquals(2, calls)
        }
}
