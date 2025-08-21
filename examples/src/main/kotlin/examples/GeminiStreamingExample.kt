package examples

import client.LlmClientFactory
import config.GeminiConfig
import kotlinx.coroutines.runBlocking
import request.GenerationRequest
import usage.LlmUsage

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
