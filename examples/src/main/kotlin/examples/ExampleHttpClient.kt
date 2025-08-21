package examples

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Example-only HTTP client builder.
 * This is not part of the library API. Consumers must bring and configure their own HttpClient.
 */
object ExampleHttpClient {
    fun createRecommendedHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 300_000
            }
            engine {
                maxConnectionsCount = 10
                pipelining = true
            }
        }
    }
}
