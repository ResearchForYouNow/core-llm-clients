package examples

import io.github.researchforyounow.llm.client.LlmClientFactory
import kotlinx.coroutines.runBlocking
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.config.OpenAiModel
import io.github.researchforyounow.llm.request.GenerationRequest
import io.github.researchforyounow.llm.usage.LlmUsage

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
                model = OpenAiModel.GPT_4_TURBO_2024_04_09,
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
