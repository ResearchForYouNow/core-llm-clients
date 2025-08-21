package response

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Content extractor for OpenAI API responses.
 */
object OpenAiContentExtractor {
    fun extractContent(
        responseJson: JsonObject,
    ): Result<String> {
        return try {
            // Check for error in the response
            val error = responseJson["error"]?.jsonObject
            if (error != null) {
                val errorMessage = error["message"]?.jsonPrimitive?.content ?: "Unknown error"
                val errorType = error["type"]?.jsonPrimitive?.content ?: "Unknown type"
                val errorCode = error["code"]?.jsonPrimitive?.content ?: "Unknown code"
                return Result.failure(Exception("OpenAI API error: $errorType ($errorCode) - $errorMessage"))
            }

            val choices = responseJson["choices"]?.jsonArray
            if (choices != null && choices.isNotEmpty()) {
                val message = choices[0].jsonObject["message"]?.jsonObject
                val content = message?.get("content")?.jsonPrimitive?.content

                if (content != null) {
                    Result.success(content)
                } else {
                    Result.failure(Exception("No content found in OpenAI response"))
                }
            } else {
                Result.failure(Exception("No choices found in OpenAI response"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error extracting content from OpenAI response", e))
        }
    }
}
