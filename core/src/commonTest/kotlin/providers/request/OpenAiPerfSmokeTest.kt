package providers.request

import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.config.ResponseFormat
import io.github.researchforyounow.llm.providers.openai.request.OpenAiRequestBuilder
import io.github.researchforyounow.llm.providers.openai.response.OpenAiContentExtractor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertTrue

class OpenAiPerfSmokeTest {
    private fun sampleResponse(): String {
        return """
            {
              "choices": [
                {
                  "index": 0,
                  "message": {"role": "assistant", "content": "Hello performance"},
                  "finish_reason": "stop"
                }
              ]
            }
            """.trimIndent()
    }

    @Test
    fun `builders and extractors run fast enough`() {
        val cfg = OpenAiConfig.defaultConfig().copy(
            apiKey = "k",
            responseFormat = ResponseFormat.TEXT,
            stopSequences = listOf("END"),
        )
        val builder = OpenAiRequestBuilder.create(cfg)
        val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val responseJson = jsonParser.parseToJsonElement(sampleResponse()).jsonObject

        val iterations = 3000
        val start = System.nanoTime()
        repeat(iterations) {
            val req = builder.buildRequest("sys", "user $it")
            // Touch a few fields
            requireNotNull(req["model"]) { "model missing" }
            val ignored = OpenAiContentExtractor.extractContent(responseJson)
        }
        val tookMs = (System.nanoTime() - start) / 1_000_000
        // Generous threshold to avoid flakiness in CI
        assertTrue(tookMs < 8000, "Performance smoke took ${'$'}tookMs ms which is too slow")
    }
}
