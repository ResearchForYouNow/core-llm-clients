package io.github.researchforyounow.llm.providers.openai.config

/**
 * Controls how streamTyped parses streaming output:
 *
 * - NDJSON_PER_LINE: parse one complete JSON object per newline (emit as soon as a line is complete)
 * - BUFFER_AND_PARSE_FINAL: accumulate all text and parse once at the end ([DONE])
 */
enum class StreamParsingMode {
    NDJSON_PER_LINE,
    BUFFER_AND_PARSE_FINAL,
}
