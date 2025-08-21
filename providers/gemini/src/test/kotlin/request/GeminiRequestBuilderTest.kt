package request

import config.GeminiConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GeminiRequestBuilderTest {
    @Test
    fun `buildRequest combines system and prompt and sets generationConfig`() {
        val cfg = GeminiConfig.defaultConfig().copy(
            temperature = 0.55,
            topK = 20,
            topP = 0.8,
            maxOutputTokens = 128,
            candidateCount = 1,
            stopSequences = listOf("END"),
        )
        val builder = GeminiRequestBuilder.create(cfg)
        val api = builder.buildRequest(systemMessage = "sys", prompt = "user")

        assertEquals(1, api.contents.size)
        val parts = api.contents.first().parts
        assertEquals(1, parts.size)
        assertEquals("sys\n\nuser", parts.first().text)

        val gen = api.generationConfig
        assertNotNull(gen)
        assertEquals(cfg.temperature, gen!!.temperature)
        assertEquals(cfg.topK, gen.topK)
        assertEquals(cfg.topP, gen.topP)
        assertEquals(cfg.maxOutputTokens, gen.maxOutputTokens)
        assertEquals(cfg.candidateCount, gen.candidateCount)
        assertEquals(cfg.stopSequences, gen.stopSequences)
    }
}
