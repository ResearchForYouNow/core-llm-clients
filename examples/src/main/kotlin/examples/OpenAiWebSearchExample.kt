package examples

import io.github.researchforyounow.llm.client.LlmClientFactory
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.config.Models
import io.github.researchforyounow.llm.providers.openai.config.ResponseFormat
import io.github.researchforyounow.llm.request.GenerationRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

object OpenAiWebSearchExample {

    @Serializable
    data class WebSearchResult(
        val answer: String,
        val sources: List<String>,
    )

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
                modelName = Models.GPT_4O_MINI_SEARCH_PREVIEW,
                responseFormat = ResponseFormat.JSON_SCHEMA,
                jsonSchemaName = "web_search_result",
                jsonSchema = """
                    {
                      "type": "object",
                      "properties": {
                        "answer": { "type": "string" },
                        "sources": { "type": "array", "items": { "type": "string" } }
                      },
                      "required": ["answer", "sources"]
                    }
                """.trimIndent(),
                enableWebSearch = true,
            )
            val client = factory.createOpenAiClient(config = config)

            val result = client.generate(
                request = GenerationRequest.of(
                    prompt = "Find the official Kotlin website and return a short answer with sources.",
                ),
                responseType = WebSearchResult::class.java,
            )

            result.onSuccess { payload ->
                println("answer=${payload.answer}")
                println("sources=${payload.sources.joinToString()}")
            }.onFailure { err ->
                println(err.message ?: "error")
            }
        }
    }
}
