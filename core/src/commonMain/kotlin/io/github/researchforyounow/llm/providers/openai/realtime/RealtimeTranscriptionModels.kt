package io.github.researchforyounow.llm.providers.openai.realtime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for creating/connecting a realtime transcription session.
 */
data class RealtimeTranscriptionSessionRequest(
    val inputAudioFormat: String = "pcm16",
    val transcriptionModel: String,
    val prompt: String? = null,
    val language: String? = "en",
    val turnDetection: RealtimeTurnDetection? = null,
    val noiseReduction: RealtimeNoiseReduction? = null,
    val include: List<String>? = null,
) {
    init {
        require(inputAudioFormat.isNotBlank()) { "inputAudioFormat cannot be blank" }
        require(transcriptionModel.isNotBlank()) { "transcriptionModel cannot be blank" }
    }
}

@Serializable
data class RealtimeTranscriptionSessionResponse(
    val id: String? = null,
    @SerialName("client_secret")
    val clientSecret: RealtimeClientSecret? = null,
    val model: String? = null,
    @SerialName("input_audio_format")
    val inputAudioFormat: String? = null,
)

@Serializable
data class RealtimeClientSecret(
    val value: String? = null,
    @SerialName("expires_at")
    val expiresAt: Long? = null,
)

data class RealtimeTurnDetection(
    val type: String = "server_vad",
    val threshold: Double? = null,
    val prefixPaddingMs: Int? = null,
    val silenceDurationMs: Int? = null,
) {
    init {
        require(type.isNotBlank()) { "turnDetection.type cannot be blank" }
    }
}

data class RealtimeNoiseReduction(
    val type: String,
) {
    init {
        require(type.isNotBlank()) { "noiseReduction.type cannot be blank" }
    }
}
