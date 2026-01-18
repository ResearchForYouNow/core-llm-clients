Speech to text
==============

This library exposes OpenAI speech-to-text through the OpenAI client:

- File transcription (`/v1/audio/transcriptions`)
- File translation (`/v1/audio/translations`)
- Realtime transcription (WebSocket, live audio)

Provider limits apply (file size, formats, model availability).

Audio file inputs
-----------------

Use `AudioFile` with raw bytes, a filename, and a content type. By default it enforces
a 25 MB size limit (`AudioFile.DEFAULT_MAX_BYTES`). Override with `maxBytes` if needed.

```kotlin
import io.github.researchforyounow.llm.providers.openai.request.AudioFile
import java.io.File

val audio = File("/path/to/audio.mp3")
val audioFile = AudioFile(
    bytes = audio.readBytes(),
    fileName = audio.name,
    contentType = "audio/mpeg",
)
```

Transcribe audio (non-streaming)
--------------------------------

```kotlin
import io.github.researchforyounow.llm.client.LlmClientFactory
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.request.AudioResponseFormat
import io.github.researchforyounow.llm.providers.openai.request.AudioTranscriptionRequest

val httpClient = ExampleHttpClient.createRecommendedHttpClient()
val factory = LlmClientFactory(httpClient = httpClient, openAiApiKey = System.getenv("OPENAI_API_KEY"))
val client = factory.createOpenAiClient(config = OpenAiConfig.defaultConfig())

val request = AudioTranscriptionRequest(
    file = audioFile,
    model = "gpt-4o-transcribe",
    responseFormat = AudioResponseFormat.TEXT,
)

val result = client.transcribe(request).getOrThrow()
println(result.text)
```

Transcribe audio (streaming results for a file)
-----------------------------------------------

```kotlin
import io.github.researchforyounow.llm.providers.openai.request.AudioTranscriptionRequest
import kotlinx.coroutines.runBlocking

runBlocking {
    val request = AudioTranscriptionRequest(
        file = audioFile,
        model = "gpt-4o-mini-transcribe",
        responseFormat = AudioResponseFormat.TEXT,
        stream = true,
    )

    client.streamTranscription(request).collect { event ->
        println(event.type)
    }
}
```

Translate audio (non-streaming)
-------------------------------

```kotlin
import io.github.researchforyounow.llm.providers.openai.request.AudioTranslationRequest

val request = AudioTranslationRequest(
    file = audioFile,
    model = "whisper-1",
    responseFormat = AudioResponseFormat.TEXT,
)

val result = client.translate(request).getOrThrow()
println(result.text)
```

Realtime transcription (live audio)
-----------------------------------

Realtime transcription accepts PCM16 audio buffers. The default format is:

- 16-bit little-endian PCM (signed)
- Mono
- 16 kHz sample rate

Your client app must capture and convert microphone input to PCM16 bytes.

```kotlin
import io.github.researchforyounow.llm.client.LlmClientFactory
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionController
import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionSessionRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets

val httpClient = HttpClient { install(WebSockets) }
val factory = LlmClientFactory(httpClient = httpClient, openAiApiKey = System.getenv("OPENAI_API_KEY"))
val client = factory.createOpenAiClient(config = OpenAiConfig.defaultConfig())

val request = RealtimeTranscriptionSessionRequest(
    transcriptionModel = "gpt-4o-mini-transcribe",
    // language defaults to "en"
)

val session = client.createRealtimeTranscriptionSession(request).getOrThrow()
val connection = client.openRealtimeTranscriptionConnection(request, session).getOrThrow()
val controller = RealtimeTranscriptionController(connection)

controller.start()
controller.appendAudio(pcm16Chunk)

// Stream output (deltas + completed)
controller.events.collect { event ->
    println(event.type)
}
```

Realtime controls
-----------------

Use these for UI buttons (start/pause/resume/stop):

```kotlin
controller.start()
controller.pause()
controller.resume()
controller.stop()
```

Realtime non-streaming (one-time transcript)
--------------------------------------------

If you do not want streaming events, wait for the next completed transcript:

```kotlin
controller.start()
// append audio buffers...
controller.stop()
val transcript = controller.awaitNextTranscript()
println(transcript)
```
