package response

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeminiContentExtractorTest {
    private fun parse(
        json: String,
    ): JsonObject =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }.parseToJsonElement(json).jsonObject

    @Test
    fun `extractContent returns cleaned text from first candidate`() {
        val json = parse(
            """
                {
                  "candidates": [
                    {"content": {"parts": [{"text": "```json\n{\"a\":1}\n```"}]}}
                  ]
                }
            """.trimIndent(),
        )
        val result = GeminiContentExtractor.extractContent(json)
        assertTrue(result.isSuccess)
        assertEquals("{\"a\":1}", result.getOrThrow())
    }

    @Test
    fun `extractContent surfaces API error`() {
        val json = parse(
            "{" + "\"error\": {\"message\": \"bad\", \"code\": \"400\"}" + "}",
        )
        val result = GeminiContentExtractor.extractContent(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Gemini API error"))
    }

    @Test
    fun `extractContent fails when no candidates`() {
        val json = parse("{}")
        val result = GeminiContentExtractor.extractContent(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `extractContent fails when no parts`() {
        val json = parse(
            """
                {"candidates":[{"content": {}}]}
            """.trimIndent(),
        )
        val result = GeminiContentExtractor.extractContent(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `extractContent returns raw text when clean disabled`() {
        val json = parse(
            """
                {
                  "candidates": [
                    {"content": {"parts": [{"text": "```json\n{\"a\":1}\n```"}]}}
                  ]
                }
            """.trimIndent(),
        )
        val result = GeminiContentExtractor.extractContent(json, cleanMarkdown = false)
        assertTrue(result.isSuccess)
        assertEquals("```json\n{\"a\":1}\n```", result.getOrThrow())
    }
}
