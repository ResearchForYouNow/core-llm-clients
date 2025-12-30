package io.github.researchforyounow.llm.providers.openai.config

/**
 * Convenience constants for commonly used OpenAI model names.
 * These are optional helpers; any string model name is allowed.
 */
object Models {
    // GPT-4o family
    const val GPT_4O: String = "gpt-4o"
    const val GPT_4O_2024_05_13: String = "gpt-4o-2024-05-13"
    const val GPT_4O_2024_08_06: String = "gpt-4o-2024-08-06"
    const val GPT_4O_MINI: String = "gpt-4o-mini"

    // GPT-4.1 family
    const val GPT_4_1: String = "gpt-4.1"
    const val GPT_4_1_MINI: String = "gpt-4.1-mini"

    // GPT-4 Turbo (legacy but common)
    const val GPT_4_TURBO_2024_04_09: String = "gpt-4-turbo-2024-04-09"
}

