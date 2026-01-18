package providers.realtime

import io.github.researchforyounow.llm.providers.openai.realtime.RealtimeTranscriptionConnection
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RealtimeTranscriptionConnectionTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `pause prevents append`() = runBlocking {
        val incoming = Channel<Frame>(Channel.UNLIMITED)
        val outgoing = Channel<Frame>(Channel.UNLIMITED)
        val session = FakeClientWebSocketSession(incoming, outgoing)
        val connection = RealtimeTranscriptionConnection(session, json)

        connection.pause()
        connection.appendAudio(byteArrayOf(1, 2, 3))

        val sent = withTimeoutOrNull(200) { outgoing.receive() }
        assertNull(sent, "No frame should be sent while paused")
    }

    @Test
    fun `stop commits audio buffer`() = runBlocking {
        val incoming = Channel<Frame>(Channel.UNLIMITED)
        val outgoing = Channel<Frame>(Channel.UNLIMITED)
        val session = FakeClientWebSocketSession(incoming, outgoing)
        val connection = RealtimeTranscriptionConnection(session, json)

        connection.stop()

        val sent = withTimeoutOrNull(500) { outgoing.receive() }
        assertNotNull(sent)
        val text = String((sent as Frame.Text).data)
        val type = json.parseToJsonElement(text).jsonObject["type"]?.jsonPrimitive?.content
        assertEquals("input_audio_buffer.commit", type)
    }

    @Test
    fun `awaitNextTranscript returns completed transcript`() = runBlocking {
        val incoming = Channel<Frame>(Channel.UNLIMITED)
        val outgoing = Channel<Frame>(Channel.UNLIMITED)
        val session = FakeClientWebSocketSession(incoming, outgoing)
        val connection = RealtimeTranscriptionConnection(session, json)

        val payload = """
            {"type":"conversation.item.input_audio_transcription.completed","transcript":"hello"}
        """.trimIndent()
        incoming.send(Frame.Text(payload))
        incoming.close()

        val transcript = connection.awaitNextTranscript()
        assertEquals("hello", transcript)
    }

    private class FakeClientWebSocketSession(
        override val incoming: ReceiveChannel<Frame>,
        override val outgoing: SendChannel<Frame>,
    ) : ClientWebSocketSession {
        private val job = Job()
        override val call: HttpClientCall = HttpClientCall(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
        )

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Default + job

        override var masking: Boolean = false
        override var maxFrameSize: Long = Long.MAX_VALUE
        override val extensions: List<WebSocketExtension<*>> = emptyList()

        override suspend fun flush() = Unit

        @Deprecated("Use cancel() instead.", level = DeprecationLevel.ERROR)
        override fun terminate() = Unit
    }
}
