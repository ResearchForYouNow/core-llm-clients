package error

import io.github.researchforyounow.llm.error.LlmError
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LlmErrorTest {
    @Test
    fun `fromException maps SocketTimeoutException to TransportError`() {
        val e = java.net.SocketTimeoutException("timed out")
        val mapped = LlmError.fromException(e)
        assertIs<LlmError.TransportError>(mapped)
        assertTrue(mapped.message!!.contains("timed out").not() || mapped.message!!.contains("Request timed out"))
    }

    @Test
    fun `fromException maps SerializationException to DeserializationError`() {
        val e = SerializationException("bad json")
        val mapped = LlmError.fromException(e)
        assertIs<LlmError.DeserializationError>(mapped)
    }

    @Test
    fun `fromException passes through LlmError`() {
        val original: LlmError = LlmError.AuthError("no auth")
        val mapped = LlmError.fromException(original)
        // Should be the same instance
        assertTrue(mapped === original)
    }

    @Test
    fun `fromException maps message indicating 401 to AuthError`() {
        val e = IllegalStateException("HTTP 401 Unauthorized")
        val mapped = LlmError.fromException(e)
        assertIs<LlmError.AuthError>(mapped)
    }

    @Test
    fun `fromException maps message indicating 429 to RateLimitError`() {
        val e = RuntimeException("429 Too Many Requests - rate limit exceeded")
        val mapped = LlmError.fromException(e)
        assertIs<LlmError.RateLimitError>(mapped)
    }

    @Test
    fun `fromException maps message indicating 400 or 422 to InvalidRequestError`() {
        val e1 = RuntimeException("400 Bad Request")
        val e2 = RuntimeException("Unprocessable 422")
        assertIs<LlmError.InvalidRequestError>(LlmError.fromException(e1))
        assertIs<LlmError.InvalidRequestError>(LlmError.fromException(e2))
    }

    @Test
    fun `fromException maps provider api error message to ProviderSpecificError`() {
        val e1 = RuntimeException("OpenAI API error: some message")
        val e2 = RuntimeException("Gemini API error: some message")
        assertIs<LlmError.ProviderSpecificError>(LlmError.fromException(e1))
        assertIs<LlmError.ProviderSpecificError>(LlmError.fromException(e2))
    }

    @Test
    fun `fromException maps unknown exception to TransportError`() {
        val e = IllegalArgumentException("weird")
        val mapped = LlmError.fromException(e)
        assertIs<LlmError.TransportError>(mapped)
    }
}
