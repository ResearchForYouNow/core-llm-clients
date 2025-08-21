package client

import config.OpenAiConfig
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

class OpenAiClientContractTest {
    private fun successJson(
        text: String,
    ): String {
        return """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "$text"
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {"prompt_tokens": 1, "completion_tokens": 2, "total_tokens": 3}
            }
            """.trimIndent()
    }

    @Test
    fun generateText_returnsExpectedText_andExposesConfig() {
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
            val config = OpenAiConfig.defaultConfig().copy(apiKey = "test-key")
            val client = OpenAiClient.builder().httpClient(http).config(config).build()

            val req = GenerationRequest.of("Say hi")
            val result = client.generateText(req)

            assertTrue(result.isSuccess)
            assertEquals("Hello world", result.getOrThrow())
            assertEquals(config.model.modelName, client.getModelName())
            assertEquals("test-key", client.getApiKey())
        }
    }

    @Test
    fun stream_emitsAllChunks_andConcatenatesToSameText() =
        runBlocking {
            val streamBody = buildString {
                append("data: {\"choices\":[{\"delta\":{\"content\":\"Hel\"}}]}\n\n")
                append("data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}\n\n")
                append("data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n\n")
                append("data: [DONE]\n\n")
            }
            val engine = MockEngine { _ ->
                respond(
                    content = streamBody,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, listOf("text/event-stream")),
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
            val config = OpenAiConfig.defaultConfig().copy(apiKey = "test-key")
            val client = OpenAiClient.builder().httpClient(http).config(config).build()

            val chunks = client.stream(GenerationRequest.of("Stream it")).toList()
            assertEquals(listOf("Hel", "lo", " world"), chunks.map { it.content })
            assertEquals("Hello world", chunks.joinToString(separator = "") { it.content })
        }

    @Test
    fun retry_onFailure_thenSuccess_withIdempotencyKey() {
        runBlocking {
            var calls = 0
            val engine = MockEngine { _ ->
                calls++
                if (calls == 1) {
                    throw java.net.SocketTimeoutException("simulated timeout")
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
            val retryingConfig = OpenAiConfig.defaultConfig().copy(
                apiKey = "key",
                // zero delays to speed up tests
                retryPolicy = client.RetryPolicy(
                    maxAttempts = 2,
                    initialDelayMillis = 0,
                    maxDelayMillis = 0,
                    jitterMillis = 0,
                ),
            )
            val client = OpenAiClient.builder().httpClient(http).config(retryingConfig).build()

            val req = GenerationRequest.builder().prompt("Try it").idempotencyKey("abc").build()

            val result = client.generateText(req)
            assertTrue(result.isSuccess, "Expected success after retry, got $result")
            assertEquals("OK after retry", result.getOrThrow())
            assertEquals(2, calls)
        }
    }
}
