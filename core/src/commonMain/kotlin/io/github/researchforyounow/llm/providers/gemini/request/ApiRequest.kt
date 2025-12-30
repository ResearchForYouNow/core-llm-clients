package io.github.researchforyounow.llm.providers.gemini.request

import kotlinx.serialization.Serializable

/**
 * Generic request model for AI content generation APIs.
 * This structure is designed to be adaptable to different AI providers.
 *
 * This class is internal and should not be used directly by consumers.
 */
@Serializable
internal data class ApiRequest(
    val contents: List<RequestContent>,
    val generationConfig: GenerationConfig? = null,
)

/**
 * Content part of the request.
 *
 * This class is internal and should not be used directly by consumers.
 */
@Serializable
internal data class RequestContent(
    val parts: List<RequestPart>,
)

/**
 * Individual part of the content request.
 *
 * This class is internal and should not be used directly by consumers.
 */
@Serializable
internal data class RequestPart(
    val text: String,
)

/**
 * Generation configuration for the API request.
 * This matches the structure expected by the Gemini API.
 *
 * This class is internal and should not be used directly by consumers.
 */
@Serializable
internal data class GenerationConfig(
    val temperature: Double,
    val topK: Int,
    val topP: Double,
    val maxOutputTokens: Int,
    val candidateCount: Int = 1,
    val stopSequences: List<String> = emptyList(),
)
