package io.github.researchforyounow.llm.client
import io.github.researchforyounow.llm.providers.gemini.client.GeminiClient
import io.github.researchforyounow.llm.providers.gemini.config.GeminiConfig
import io.github.researchforyounow.llm.providers.openai.client.OpenAiClient
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.ktor.client.HttpClient

/**
 * Factory for creating provider-specific LLM clients with shared credentials and HTTP client.
 * Instantiate once and reuse across your application for dependency injection.
 */
class LlmClientFactory(
    private val httpClient: HttpClient,
    private val openAiApiKey: String? = null,
    private val geminiApiKey: String? = null,
) {
    fun createOpenAiClient(
        config: OpenAiConfig = OpenAiConfig.defaultConfig(),
    ): OpenAiClient {
        val key = requireNotNull(openAiApiKey) { "OpenAI API key must be provided" }
        return OpenAiClient.builder()
            .config(config.withApiKey(key))
            .httpClient(httpClient)
            .build()
    }

    fun createGeminiClient(
        config: GeminiConfig = GeminiConfig.defaultConfig(),
    ): GeminiClient {
        val key = requireNotNull(geminiApiKey) { "Gemini API key must be provided" }
        return GeminiClient.builder()
            .config(config.withApiKey(key))
            .httpClient(httpClient)
            .build()
    }
}
