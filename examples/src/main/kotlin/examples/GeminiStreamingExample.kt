package examples

import io.github.researchforyounow.llm.client.LlmClientFactory
import io.github.researchforyounow.llm.providers.gemini.config.GeminiConfig
import io.github.researchforyounow.llm.request.GenerationRequest
import io.github.researchforyounow.llm.usage.LlmUsage
import kotlinx.coroutines.runBlocking

/**
 * Minimal streaming example for the Gemini provider.
 * Replace the apiKey with your own before running.
 */
object GeminiStreamingExample {
    @JvmStatic
    fun main(
        args: Array<String>,
    ) {
        runBlocking(block = {
            val apiKey = ""
            require(apiKey.isNotBlank()) { "GEMINI_API_KEY must be set" }

            val httpClient = ExampleHttpClient.createRecommendedHttpClient()
            val factory = LlmClientFactory(httpClient = httpClient, geminiApiKey = apiKey)
            val config = GeminiConfig(
                usageSink = { usage: LlmUsage ->
                    println(message = "\n[usage] prompt=${usage.promptTokens}")
                    println(message = " completion=${usage.completionTokens}")
                },
            )
            val client = factory.createGeminiClient(config = config)

            println(message = "Streaming response:")
            val flow = client.stream(request = GenerationRequest.of(prompt = "Write a short limerick about flows."))
            flow.collect { chunk ->
                print(message = chunk.content)
            }
            println()
        })
    }
}
