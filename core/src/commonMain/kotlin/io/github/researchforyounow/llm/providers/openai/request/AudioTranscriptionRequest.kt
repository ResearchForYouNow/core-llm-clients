package io.github.researchforyounow.llm.providers.openai.request

/**
 * Configuration for OpenAI audio transcription requests.
 */
data class AudioTranscriptionRequest(
    val file: AudioFile,
    val model: String,
    val responseFormat: AudioResponseFormat = AudioResponseFormat.JSON,
    val prompt: String? = null,
    val language: String? = null,
    val temperature: Double? = null,
    val timestampGranularities: List<String>? = null,
    val chunkingStrategy: String? = null,
    val include: List<String>? = null,
    val knownSpeakerNames: List<String>? = null,
    val knownSpeakerReferences: List<String>? = null,
    val stream: Boolean = false,
    val idempotencyKey: String? = null,
    val tags: Map<String, String>? = null,
) {
    init {
        require(model.isNotBlank()) { "Model name cannot be blank" }
        knownSpeakerNames?.let { require(it.size <= 4) { "knownSpeakerNames supports up to 4 entries" } }
        knownSpeakerReferences?.let { require(it.size <= 4) { "knownSpeakerReferences supports up to 4 entries" } }
        if (knownSpeakerNames != null && knownSpeakerReferences != null) {
            require(knownSpeakerNames.size == knownSpeakerReferences.size) {
                "knownSpeakerNames and knownSpeakerReferences must have the same size"
            }
        }
    }
}
