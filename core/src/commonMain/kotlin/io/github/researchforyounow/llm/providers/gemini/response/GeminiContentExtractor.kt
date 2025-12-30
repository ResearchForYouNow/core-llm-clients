package io.github.researchforyounow.llm.providers.gemini.response

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Content extractor for Gemini API responses.
 */
object GeminiContentExtractor {
    fun extractContent(
        responseJson: JsonObject,
    ): Result<String> {
        return extractContent(
            responseJson = responseJson,
            cleanMarkdown = true,
        )
    }

    fun extractContent(
        responseJson: JsonObject,
        cleanMarkdown: Boolean,
    ): Result<String> {
        return try {
            val error = responseJson["error"]?.jsonObject
            if (error != null) {
                val errorMessage = error["message"]?.jsonPrimitive?.content ?: "Unknown error"
                val errorCode = error["code"]?.jsonPrimitive?.content ?: "Unknown code"
                return Result.failure(Exception("Gemini API error: $errorCode - $errorMessage"))
            }

            val candidates = responseJson["candidates"]?.jsonArray
            if (candidates != null && candidates.isNotEmpty()) {
                val content = candidates[0].jsonObject["content"]?.jsonObject
                val parts = content?.get("parts")?.jsonArray

                if (parts != null && parts.isNotEmpty()) {
                    val rawText = parts[0].jsonObject["text"]?.jsonPrimitive?.content
                    if (rawText != null) {
                        val normalized = rawText.trimIndent().lines().joinToString("\n") { it.trim() }
                        val resultText = if (cleanMarkdown) cleanMarkdownCodeBlocks(normalized) else normalized
                        Result.success(resultText)
                    } else {
                        Result.failure(Exception("No text content found in Gemini response"))
                    }
                } else {
                    Result.failure(Exception("No parts found in Gemini response"))
                }
            } else {
                Result.failure(Exception("No candidates found in Gemini response"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error extracting content from Gemini response", e))
        }
    }

    private fun cleanMarkdownCodeBlocks(
        text: String,
    ): String {
        return text
            .trim()
            .replace(Regex("^```\\w*\\s*"), "")
            .replace(Regex("\\s*```$"), "")
            .trim()
    }
}
