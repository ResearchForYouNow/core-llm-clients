package providers.openai

import io.github.researchforyounow.llm.providers.openai.client.SseLineAccumulator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SseLineAccumulatorTest {

    @Test
    fun `accumulates multi-line data`() {
        val acc = SseLineAccumulator()
        assertNull(acc.onLine("data: {\"a\":1}"))
        assertNull(acc.onLine("data: {\"b\":2}"))
        val payload = acc.onLine("")
        assertEquals("{\"a\":1}\n{\"b\":2}", payload)
    }

    @Test
    fun `ignores non-data lines`() {
        val acc = SseLineAccumulator()
        assertNull(acc.onLine(": comment"))
        assertNull(acc.onLine("event: message"))
        assertNull(acc.onLine("data: [DONE]"))
        val payload = acc.onLine("")
        assertEquals("[DONE]", payload)
    }
}
