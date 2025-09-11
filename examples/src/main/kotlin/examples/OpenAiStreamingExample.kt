package examples

import io.github.researchforyounow.llm.client.LlmClientFactory
import kotlinx.coroutines.runBlocking
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.request.GenerationRequest
import io.github.researchforyounow.llm.usage.LlmUsage

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
