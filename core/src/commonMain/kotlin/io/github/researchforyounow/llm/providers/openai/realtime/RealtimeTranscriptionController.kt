package io.github.researchforyounow.llm.providers.openai.realtime

/**
 * Lightweight controller for UI-style start/pause/resume/stop interactions.
 */
class RealtimeTranscriptionController(
    private val connection: RealtimeTranscriptionConnection,
) {
    val events = connection.events

    fun start() = connection.start()

    fun pause() = connection.pause()

    fun resume() = connection.resume()

    suspend fun appendAudio(bytes: ByteArray) = connection.appendAudio(bytes)

    suspend fun stop() = connection.stop()

    suspend fun awaitNextTranscript(): String? = connection.awaitNextTranscript()

    suspend fun close() = connection.close()
}
