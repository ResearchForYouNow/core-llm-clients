package examples

import io.github.researchforyounow.llm.client.LlmClientFactory
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.request.AudioFile
import io.github.researchforyounow.llm.providers.openai.request.AudioResponseFormat
import io.github.researchforyounow.llm.providers.openai.request.AudioTranscriptionRequest
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Minimal example to verify OpenAI speech-to-text transcription.
 *
 * How to run:
 * 1) Set your OpenAI API key below (apiKey variable) or wire it from your own config management.
 * 2) Update audioPath to point at a local audio file (mp3, wav, m4a, webm, etc.).
 * 3) Run this file's main.
 */
object OpenAiSpeechToTextExample {

    @JvmStatic
    fun main(
        args: Array<String>,
    ) {
        runBlocking {
            val apiKey = ""
            require(apiKey.isNotBlank()) { "OPENAI_API_KEY must be set" }

            val audioPath = "examples/src/main/resources/samples/sample_speaking_voice.mp3"
            val audioFile = File(audioPath)
            require(audioFile.exists()) { "Audio file not found at: $audioPath" }

            val httpClient = ExampleHttpClient.createRecommendedHttpClient()
            val factory = LlmClientFactory(httpClient = httpClient, openAiApiKey = apiKey)
            val client = factory.createOpenAiClient(config = OpenAiConfig.defaultConfig())

            val request = AudioTranscriptionRequest(
                file = AudioFile(
                    bytes = audioFile.readBytes(),
                    fileName = audioFile.name,
                    contentType = "audio/mpeg",
                ),
                model = "gpt-4o-mini-transcribe",
                responseFormat = AudioResponseFormat.TEXT,
            )

            val result = client.transcribe(request)
            println(result.getOrElse { it.message ?: "error" })
        }
    }
}
