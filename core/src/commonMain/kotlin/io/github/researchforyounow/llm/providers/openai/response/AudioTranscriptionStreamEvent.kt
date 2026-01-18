package io.github.researchforyounow.llm.providers.openai.response

import kotlinx.serialization.json.JsonObject

/**
 * Wrapper for streamed transcription events.
 */
data class AudioTranscriptionStreamEvent(
    val type: String?,
    val payload: JsonObject,
)
