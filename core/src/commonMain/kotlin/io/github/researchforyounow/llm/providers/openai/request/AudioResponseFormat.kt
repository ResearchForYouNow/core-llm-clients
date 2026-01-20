package io.github.researchforyounow.llm.providers.openai.request

/**
 * Supported response formats for OpenAI audio endpoints.
 */
enum class AudioResponseFormat(
    val apiValue: String,
) {
    JSON("json"),
    TEXT("text"),
    SRT("srt"),
    VTT("vtt"),
    VERBOSE_JSON("verbose_json"),
    DIARIZED_JSON("diarized_json"),
}
