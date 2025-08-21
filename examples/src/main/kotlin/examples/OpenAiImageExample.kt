package examples

import client.LlmClientFactory
import config.OpenAiConfig
import kotlinx.coroutines.runBlocking
import request.ImageGenerationRequest
import request.ImageResponseFormat
import request.OpenAiImageModel
import java.util.UUID

/**
 * Minimal example to verify OpenAI image generation.
 *
 * It generates a 1260x720 image with ONLY the three words "India Heavy Rainfalls"
 * on a pure black background.
 *
 * How to run:
 * 1) Set your OpenAI API key below (apiKey variable) or wire it from your own config management.
 * 2) Run this file's main.
 */
object OpenAiImageExample {
    @JvmStatic
    fun main(
        args: Array<String>,
    ) {
        runBlocking {
            val apiKey = ""
            val httpClient = ExampleHttpClient.createRecommendedHttpClient()
            val factory = LlmClientFactory(httpClient = httpClient, openAiApiKey = apiKey)
            val config = OpenAiConfig.defaultConfig()
            val client = factory.createOpenAiClient(config)

            val prompt = """
                Create a happy cat..
            """.trimIndent()

            // Note: OpenAI APIs only accept certain sizes per model. 1260x720 is NOT valid and can cause HTTP 400.
            // For a widescreen-like layout, use DALL·E 3 with 1792x1024 (approx 16:9).
            val request = ImageGenerationRequest(
                prompt = prompt,
                n = 1,
                size = "1024x1024",
                responseFormat = ImageResponseFormat.URL,
                model = OpenAiImageModel.DALL_E_3,
                idempotencyKey = UUID.randomUUID().toString(),
            )

            val result = client.generateImage(request)
            if (result.isSuccess) {
                val images = result.getOrThrow()
                if (images.isEmpty()) {
                    println("No images returned by the API.")
                } else {
                    images.forEachIndexed { index, img ->
                        val label = index + 1
                        val urlOrInfo = img.url ?: "[base64 image returned: ${img.b64Json?.length ?: 0} chars]"
                        println("Image #$label: $urlOrInfo")
                    }
                }
            } else {
                val err = result.exceptionOrNull()
                when (err) {
                    is error.LlmError.ProviderHttpError -> {
                        System.err.println("Image generation failed: status=${err.statusCode} body=${err.body}")
                    }
                    else -> System.err.println("Image generation failed: ${err?.message}")
                }
            }
        }
    }
}
