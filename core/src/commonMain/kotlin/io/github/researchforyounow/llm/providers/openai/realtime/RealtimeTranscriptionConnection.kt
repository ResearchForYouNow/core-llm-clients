package io.github.researchforyounow.llm.providers.openai.realtime

import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Live transcription WebSocket connection with helpers for sending audio buffers.
 */
class RealtimeTranscriptionConnection internal constructor(
    private val session: ClientWebSocketSession,
    private val json: Json,
    val sessionId: String? = null,
) {
    private val logger = LoggerFactory.getLogger(RealtimeTranscriptionConnection::class.java)
    private val acceptingAudio = AtomicBoolean(true)

    val events: Flow<RealtimeTranscriptionEvent> = flow {
        for (frame in session.incoming) {
            val text = when (frame) {
                is Frame.Text -> frame.readText()
                else -> null
            } ?: continue
            try {
                val obj = json.parseToJsonElement(text).jsonObject
                val type = obj["type"]?.jsonPrimitive?.contentOrNull
                emit(RealtimeTranscriptionEvent(type = type, payload = obj))
            } catch (_: Exception) {
                logger.debug("Ignoring malformed realtime transcription event")
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun appendAudio(
        bytes: ByteArray,
    ) {
        if (!acceptingAudio.get()) return
        logger.debug("Appending audio buffer bytes={}", bytes.size)
        val b64 = Base64.encode(bytes)
        sendEvent(
            buildJsonObject {
                put("type", "input_audio_buffer.append")
                put("audio", b64)
            },
        )
    }

    suspend fun commitAudio() {
        logger.debug("Committing audio buffer")
        sendEvent(
            buildJsonObject {
                put("type", "input_audio_buffer.commit")
            },
        )
    }

    suspend fun clearAudio() {
        logger.debug("Clearing audio buffer")
        sendEvent(
            buildJsonObject {
                put("type", "input_audio_buffer.clear")
            },
        )
    }

    suspend fun sendEvent(
        event: JsonObject,
    ) {
        session.send(Frame.Text(json.encodeToString(JsonObject.serializer(), event)))
    }

    fun start() {
        logger.debug("Starting audio capture")
        acceptingAudio.set(true)
    }

    fun pause() {
        logger.debug("Pausing audio capture")
        acceptingAudio.set(false)
    }

    fun resume() {
        logger.debug("Resuming audio capture")
        acceptingAudio.set(true)
    }

    suspend fun stop() {
        logger.debug("Stopping audio capture")
        acceptingAudio.set(false)
        commitAudio()
    }

    /**
     * Waits for the next completed transcription and returns its transcript text.
     * Only use this if you are not already collecting from [events].
     */
    suspend fun awaitNextTranscript(): String? {
        return events.firstOrNull { it.type == "conversation.item.input_audio_transcription.completed" }
            ?.payload
            ?.get("transcript")
            ?.jsonPrimitive
            ?.contentOrNull
    }

    suspend fun close() {
        logger.debug("Closing realtime transcription connection")
        session.close()
    }
}

data class RealtimeTranscriptionEvent(
    val type: String?,
    val payload: JsonObject,
)
