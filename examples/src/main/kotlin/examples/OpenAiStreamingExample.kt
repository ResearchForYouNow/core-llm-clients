package examples

import client.LlmClientFactory
import config.OpenAiConfig
import kotlinx.coroutines.runBlocking
import request.GenerationRequest
import usage.LlmUsage

/**
 * Minimal streaming example for the OpenAI provider.
 * Replace the apiKey with your own before running.
 */
object OpenAiStreamingExample {
    @JvmStatic
    fun main(
        args: Array<String>,
    ) {
        runBlocking(block = {
            val apiKey = ""
            require(apiKey.isNotBlank()) { "OPENAI_API_KEY must be set" }

            val httpClient = ExampleHttpClient.createRecommendedHttpClient()
            val factory = LlmClientFactory(httpClient = httpClient, openAiApiKey = apiKey)
            val config = OpenAiConfig(
                usageSink = { usage: LlmUsage ->
                    println(message = "\n[usage] prompt=${usage.promptTokens}")
                    println(message = " completion=${'$'}{usage.completionTokens}")
                },
            )
            val client = factory.createOpenAiClient(config = config)

            println(message = "Streaming response:")
            val flow = client.stream(
                request = GenerationRequest.of(prompt = "Write a 100 word story on about Kotlin coroutines."),
            )
            flow.collect { chunk ->
                print(message = chunk.content)
            }
            println()
        })
    }
}
