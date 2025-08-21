package response

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAiContentExtractorTest {
    private fun parse(
        json: String,
    ): JsonObject {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }.parseToJsonElement(json).jsonObject
    }

    @Test
    fun `extractContent returns text from first choice`() {
        val json = parse(
            """
                {
                  "choices": [
                    {
                      "index": 0,
                      "message": {"role": "assistant", "content": "Hello"},
                      "finish_reason": "stop"
                    }
                  ]
                }
            """.trimIndent(),
        )
        val result = OpenAiContentExtractor.extractContent(json)
        assertTrue(result.isSuccess)
        assertEquals("Hello", result.getOrThrow())
    }

    @Test
    fun `extractContent surfaces API error`() {
        val json = parse(
            """
                {"error": {"message": "bad", "type": "invalid", "code": "400"}}
            """.trimIndent(),
        )
        val result = OpenAiContentExtractor.extractContent(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("OpenAI API error"))
    }

    @Test
    fun `extractContent fails when no choices`() {
        val json = parse("{}")
        val result = OpenAiContentExtractor.extractContent(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `extractContent fails when malformed`() {
        // Missing message/content structure
        val json = parse(
            """{"choices":[{"index":0,"finish_reason":"stop"}]}""",
        )
        val result = OpenAiContentExtractor.extractContent(json)
        assertTrue(result.isFailure)
    }
}
