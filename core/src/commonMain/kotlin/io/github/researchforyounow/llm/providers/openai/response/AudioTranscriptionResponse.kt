package io.github.researchforyounow.llm.providers.openai.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload for OpenAI audio transcription/translation requests.
 * Fields are optional to support multiple response formats.
 */
@Serializable
data class AudioTranscriptionResponse(
    val text: String? = null,
    val language: String? = null,
    val duration: Double? = null,
    val segments: List<AudioSegment>? = null,
    val words: List<AudioWord>? = null,
)

@Serializable
data class AudioSegment(
    val id: Int? = null,
    val seek: Int? = null,
    val start: Double? = null,
    val end: Double? = null,
    val text: String? = null,
    val tokens: List<Int>? = null,
    val temperature: Double? = null,
    @SerialName("avg_logprob")
    val avgLogprob: Double? = null,
    @SerialName("compression_ratio")
    val compressionRatio: Double? = null,
    @SerialName("no_speech_prob")
    val noSpeechProb: Double? = null,
    val speaker: String? = null,
)

@Serializable
data class AudioWord(
    val word: String? = null,
    val start: Double? = null,
    val end: Double? = null,
    val confidence: Double? = null,
)
