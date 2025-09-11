package io.github.researchforyounow.llm.providers.openai.config

/**
 * Enumeration of supported OpenAI models.
 *
 * Note: These are model identifiers accepted by the chat.completions API.
 * Some legacy or preview identifiers are included for compatibility.
 */
enum class OpenAiModel(
    val modelName: String,
) {
    // GPT-4 family
    GPT_4("gpt-4"),
    GPT_4_0613("gpt-4-0613"),
    GPT_4_32K("gpt-4-32k"),
    GPT_4_1106_PREVIEW("gpt-4-1106-preview"),
    GPT_4_0125_PREVIEW("gpt-4-0125-preview"),
    GPT_4_TURBO("gpt-4-turbo"),
    GPT_4_TURBO_2024_04_09("gpt-4-turbo-2024-04-09"),

    // GPT-4o family
    GPT_4O("gpt-4o"),
    GPT_4O_2024_05_13("gpt-4o-2024-05-13"),
    GPT_4O_MINI("gpt-4o-mini"),

    // GPT-4.1 family
    GPT_4_1("gpt-4.1"),
    GPT_4_1_MINI("gpt-4.1-mini"),

    // TODO - Add support for deep research models
    // Reasoning models
    // O3("o3"),
    // O3_MINI("o3-mini"),

    // GPT-3.5 family (legacy)
    GPT_3_5_TURBO("gpt-3.5-turbo"),
    GPT_3_5_TURBO_16K("gpt-3.5-turbo-16k"),
}
