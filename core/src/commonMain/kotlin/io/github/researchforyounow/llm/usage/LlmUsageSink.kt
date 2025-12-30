package io.github.researchforyounow.llm.usage

/**
 * Hook for observing normalized token usage from LLM providers.
 */
typealias LlmUsageSink = (LlmUsage) -> Unit
