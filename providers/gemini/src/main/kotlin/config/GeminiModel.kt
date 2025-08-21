package config

/**
 * Enumeration of supported Gemini models.
 */
enum class GeminiModel(
    val modelName: String,
) {
    GEMINI_1_5_FLASH_LATEST("gemini-1.5-flash-latest"),
    GEMINI_1_5_PRO_LATEST("gemini-1.5-pro-latest"),
}
