package examples

import client.LlmClientFactory
import config.GeminiConfig
import config.GeminiModel
import kotlinx.coroutines.runBlocking
import request.GenerationRequest
import usage.LlmUsage

object GeminiUsageExample {
    @JvmStatic
    fun main(
        args: Array<String>,
    ) {
        runBlocking {
            val apiKey = ""
            require(apiKey.isNotBlank()) { "GEMINI_API_KEY must be set" }
            val httpClient = ExampleHttpClient.createRecommendedHttpClient()
            val factory = LlmClientFactory(httpClient = httpClient, geminiApiKey = apiKey)
            val config = GeminiConfig(
                usageSink = { usage: LlmUsage ->
                    println(message = "prompt=${usage.promptTokens} completion=${usage.completionTokens}")
                },
                model = GeminiModel.GEMINI_1_5_FLASH_LATEST
            )
            val client = factory.createGeminiClient(config = config)

            val result = client.generateText(
                request = GenerationRequest.of(
                    prompt = "Say hello in one short sentence.",
                ),
            )
            println(message = result.getOrElse { it.message ?: "error" })
        }
    }
}
