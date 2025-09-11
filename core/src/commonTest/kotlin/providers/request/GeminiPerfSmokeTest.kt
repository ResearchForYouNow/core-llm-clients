package request

import io.github.researchforyounow.llm.providers.gemini.config.GeminiConfig
import io.github.researchforyounow.llm.providers.gemini.request.GeminiRequestBuilder
import io.github.researchforyounow.llm.providers.gemini.response.GeminiContentExtractor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertTrue

class GeminiPerfSmokeTest {
    private fun sampleResponse(): String =
        """
        {
          "candidates": [
            {"content": {"parts": [{"text": "Hello performance"}]}}
          ]
        }
        """.trimIndent()

    @Test
    fun `builders and extractors run fast enough`() {
        val cfg = GeminiConfig.defaultConfig()
        val builder = GeminiRequestBuilder.create(cfg)
        val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val responseJson = jsonParser.parseToJsonElement(sampleResponse()).jsonObject

        val iterations = 3000
        val start = System.nanoTime()
        repeat(iterations) {
            val req = builder.buildRequest(systemMessage = "sys", prompt = "user $it")
            // Touch a few fields
            require(req.contents.isNotEmpty())
            val ignored = GeminiContentExtractor.extractContent(responseJson)
        }
        val tookMs = (System.nanoTime() - start) / 1_000_000
        // Generous threshold to avoid flakiness in CI
        assertTrue(tookMs < 8000, "Performance smoke took ${'$'}tookMs ms which is too slow")
    }
}
