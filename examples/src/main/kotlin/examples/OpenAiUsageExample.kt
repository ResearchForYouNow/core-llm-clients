package examples

import client.LlmClientFactory
import config.OpenAiConfig
import config.OpenAiModel
import kotlinx.coroutines.runBlocking
import request.GenerationRequest
import usage.LlmUsage

object OpenAiUsageExample {
    @JvmStatic
    fun main(
        args: Array<String>,
    ) {
        runBlocking {
            val apiKey = ""
            require(apiKey.isNotBlank()) { "OPENAI_API_KEY must be set" }
            val httpClient = ExampleHttpClient.createRecommendedHttpClient()
            val factory = LlmClientFactory(httpClient = httpClient, openAiApiKey = apiKey)
            val config = OpenAiConfig(
                usageSink = { usage: LlmUsage ->
                    println(message = "prompt=${usage.promptTokens} completion=${usage.completionTokens}")
                },
                model = OpenAiModel.GPT_4_TURBO_2024_04_09
            )
            val client = factory.createOpenAiClient(config = config)

            val result = client.generateText(
                request = GenerationRequest.of(
                    prompt = "Say hello in one short sentence.",
                ),
            )
            println(message = result.getOrElse { it.message ?: "error" })
        }
    }
}
