package client

import config.OpenAiConfig
import error.LlmError
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
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenAiErrorMappingTest {
    @Test
    fun generateText_maps429_toRateLimitError() {
        runBlocking {
            val engine = MockEngine { _ ->
                respond(
                    content = "{\"error\":{\"message\":\"rate limit\"}}",
                    status = HttpStatusCode.TooManyRequests,
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
            val config = OpenAiConfig.defaultConfig().copy(apiKey = "key")
            val client = OpenAiClient.builder().httpClient(http).config(config).build()

            val result = client.generateText(GenerationRequest.of("hi"))
            assertTrue(result.isFailure)
            val err = result.exceptionOrNull()
            assertIs<LlmError.RateLimitError>(err)
        }
    }

    @Test
    fun generateText_maps500_toProviderHttpError() {
        runBlocking {
            val engine = MockEngine { _ ->
                respond(
                    content = "{\"error\":{\"message\":\"server\"}}",
                    status = HttpStatusCode.InternalServerError,
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
            val config = OpenAiConfig.defaultConfig().copy(apiKey = "key")
            val client = OpenAiClient.builder().httpClient(http).config(config).build()

            val result = client.generateText(GenerationRequest.of("hi"))
            assertTrue(result.isFailure)
            val err = result.exceptionOrNull()
            assertIs<LlmError.ProviderHttpError>(err)
        }
    }

    @Test
    fun stream_maps429_toRateLimitError() {
        runBlocking {
            val engine = MockEngine { _ ->
                respond(
                    content = "Too many",
                    status = HttpStatusCode.TooManyRequests,
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
            val config = OpenAiConfig.defaultConfig().copy(apiKey = "key")
            val client = OpenAiClient.builder().httpClient(http).config(config).build()

            assertFailsWith<LlmError.RateLimitError> {
                client.stream(GenerationRequest.of("hi")).toList()
            }
        }
    }
}
