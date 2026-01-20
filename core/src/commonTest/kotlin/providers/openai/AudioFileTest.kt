package providers.openai

import io.github.researchforyounow.llm.providers.openai.request.AudioFile
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AudioFileTest {

    @Test
    fun `audio file enforces max bytes`() {
        assertFailsWith<IllegalArgumentException> {
            AudioFile(bytes = ByteArray(3), fileName = "a.wav", maxBytes = 2)
        }
    }
}
