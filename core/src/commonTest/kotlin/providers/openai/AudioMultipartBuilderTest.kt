package providers.openai

import io.github.researchforyounow.llm.providers.openai.request.AudioFile
import io.github.researchforyounow.llm.providers.openai.request.AudioMultipartBuilder
import io.github.researchforyounow.llm.providers.openai.request.AudioResponseFormat
import io.github.researchforyounow.llm.providers.openai.request.AudioTranscriptionRequest
import io.github.researchforyounow.llm.providers.openai.request.AudioTranslationRequest
import io.ktor.http.content.PartData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AudioMultipartBuilderTest {

    @Test
    fun `buildTranscriptionParts includes file and fields`() {
        val file = AudioFile(bytes = byteArrayOf(1, 2, 3), fileName = "audio.wav", contentType = "audio/wav")
        val request = AudioTranscriptionRequest(
            file = file,
            model = "gpt-4o-mini-transcribe",
            responseFormat = AudioResponseFormat.TEXT,
            prompt = "hint",
            language = "en",
            timestampGranularities = listOf("word"),
            stream = true,
        )

        val parts = AudioMultipartBuilder.buildTranscriptionParts(request, stream = true)

        val filePart = parts.filterIsInstance<PartData.FileItem>().firstOrNull()
        assertNotNull(filePart)
        assertEquals("file", filePart.name)
        assertEquals("audio.wav", filePart.originalFileName)
        assertEquals("audio/wav", filePart.contentType?.toString())

        assertEquals("gpt-4o-mini-transcribe", parts.formValue("model"))
        assertEquals("text", parts.formValue("response_format"))
        assertEquals("hint", parts.formValue("prompt"))
        assertEquals("en", parts.formValue("language"))
        assertEquals("word", parts.formValue("timestamp_granularities[]"))
        assertEquals("true", parts.formValue("stream"))
    }

    @Test
    fun `buildTranslationParts includes file and fields`() {
        val file = AudioFile(bytes = byteArrayOf(9, 9), fileName = "audio.mp3", contentType = "audio/mpeg")
        val request = AudioTranslationRequest(
            file = file,
            model = "whisper-1",
            responseFormat = AudioResponseFormat.TEXT,
            prompt = "translate",
        )

        val parts = AudioMultipartBuilder.buildTranslationParts(request)

        val filePart = parts.filterIsInstance<PartData.FileItem>().firstOrNull()
        assertNotNull(filePart)
        assertEquals("file", filePart.name)
        assertEquals("audio.mp3", filePart.originalFileName)
        assertEquals("audio/mpeg", filePart.contentType?.toString())

        assertEquals("whisper-1", parts.formValue("model"))
        assertEquals("text", parts.formValue("response_format"))
        assertEquals("translate", parts.formValue("prompt"))
    }

    private fun List<PartData>.formValue(
        name: String,
    ): String? = filterIsInstance<PartData.FormItem>().firstOrNull { it.name == name }?.value
}
