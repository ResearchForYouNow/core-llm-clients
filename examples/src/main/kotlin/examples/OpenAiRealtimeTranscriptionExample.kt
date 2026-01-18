package examples

import io.github.researchforyounow.llm.client.LlmClientFactory
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionController
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionSessionRequest
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTurnDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Minimal example to verify OpenAI realtime transcription via WebSocket.
 *
 * How to run:
 * 1) Set your OpenAI API key below (apiKey variable) or wire it from your own config management.
 * 2) Run this file's main.
 */
object OpenAiRealtimeTranscriptionExample {

    @JvmStatic
    fun main(
        args: Array<String>,
    ) {
        runBlocking {
            val apiKey = ""
            require(apiKey.isNotBlank()) { "OPENAI_API_KEY must be set" }

            val httpClient = ExampleHttpClient.createRecommendedHttpClient()
            val factory = LlmClientFactory(httpClient = httpClient, openAiApiKey = apiKey)
            val client = factory.createOpenAiClient(config = OpenAiConfig.defaultConfig())

            val useServerVad = true
            val request = RealtimeTranscriptionSessionRequest(
                inputAudioFormat = "pcm16",
                transcriptionModel = "gpt-4o-mini-transcribe",
                language = "en",
                turnDetection = if (useServerVad) RealtimeTurnDetection(type = "server_vad") else null,
            )

            val session = client.createRealtimeTranscriptionSession(request).getOrThrow()
            val connection = client.openRealtimeTranscriptionConnection(
                request = request,
                session = session,
            ).getOrThrow()
            val controller = RealtimeTranscriptionController(connection)
            controller.start()

            val streaming = true
            val pauseAtSeconds = 8
            val pauseDurationSeconds = 2

            val lastTranscript = AtomicReference<String?>(null)
            val eventsJob = launch {
                controller.events.collect { event ->
                    val delta = event.payload["delta"]?.jsonPrimitive?.contentOrNull
                    val text = event.payload["text"]?.jsonPrimitive?.contentOrNull
                    val type = event.type ?: "unknown"
                    if (type == "conversation.item.input_audio_transcription.completed") {
                        val transcript = event.payload["transcript"]?.jsonPrimitive?.contentOrNull
                        if (!transcript.isNullOrBlank()) {
                            lastTranscript.set(transcript)
                        }
                    }
                    if (streaming) {
                        when {
                            delta != null -> println("event=$type delta=$delta")
                            text != null -> println("event=$type text=$text")
                            else -> println("event=$type payload=${event.payload}")
                        }
                    }
                }
            }

            val controlJob = launch {
                delay(pauseAtSeconds * 1_000L)
                println("Pausing audio capture for $pauseDurationSeconds seconds...")
                controller.pause()
                delay(pauseDurationSeconds * 1_000L)
                println("Resuming audio capture...")
                controller.resume()
            }

            val audioJob = launch(Dispatchers.IO) {
                val format = AudioFormat(
                    16000f, // sampleRate
                    16, // sampleSizeInBits
                    1, // channels
                    true, // signed
                    false, // bigEndian
                )

                val info = DataLine.Info(TargetDataLine::class.java, format)
                val line = AudioSystem.getLine(info) as TargetDataLine
                line.open(format)
                line.start()

                val buffer = ByteArray(3200) // ~100ms of audio at 16kHz mono PCM16
                val maxSeconds = if (streaming) 30 else 15
                val startMs = System.currentTimeMillis()
                println("Listening for $maxSeconds seconds... speak into your microphone.")
                while (System.currentTimeMillis() - startMs < maxSeconds * 1000L) {
                    val read = line.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                        connection.appendAudio(chunk)
                    }
                }

                line.stop()
                line.close()
            }

            audioJob.join()
            controller.stop()

            if (!streaming) {
                val transcript = withTimeoutOrNull(10_000) {
                    controller.awaitNextTranscript()
                }
                val finalTranscript = transcript?.takeIf { it.isNotBlank() } ?: lastTranscript.get()
                if (finalTranscript.isNullOrBlank()) {
                    println("final transcript: <none>")
                } else {
                    println("final transcript: $finalTranscript")
                }
            }

            controller.close()
            controlJob.cancelAndJoin()
            eventsJob.cancelAndJoin()
        }
    }
}
