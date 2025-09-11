package io.github.researchforyounow.llm.providers.openai.request

/**
 * Enumeration of supported OpenAI image generation models.
 */
enum class OpenAiImageModel(
    val modelName: String,
) {
    GPT_IMAGE_1("gpt-image-1"),
    DALL_E_3("dall-e-3"),
}
