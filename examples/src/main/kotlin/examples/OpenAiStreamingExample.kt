@file:Suppress("ktlint")
package examples

import io.github.researchforyounow.llm.client.LlmClientFactory
import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.config.ResponseFormat
import io.github.researchforyounow.llm.providers.openai.config.StreamParsingMode
import io.github.researchforyounow.llm.request.GenerationRequest
import io.github.researchforyounow.llm.usage.LlmUsage
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

private val apiKey = ""

/**
 * Minimal streaming example for the OpenAI provider.
 * Replace the apiKey with your own before running.
 */
object OpenAiStreamingExample {
    @JvmStatic
    fun main(
        args: Array<String>,
    ) {
        runBlocking(block = {
            require(apiKey.isNotBlank()) { "OPENAI_API_KEY must be set" }

            val httpClient = ExampleHttpClient.createRecommendedHttpClient()
            val factory = LlmClientFactory(httpClient = httpClient, openAiApiKey = apiKey)
            val config = OpenAiConfig(
                usageSink = { usage: LlmUsage ->
                    println(message = "\n[usage] prompt=${usage.promptTokens}")
                    println(message = " completion=${'$'}{usage.completionTokens}")
                },
                modelName = OpenAiConfig.DEFAULT_MODEL_NAME,
            )
            val client = factory.createOpenAiClient(config = config)

            println(message = "Streaming response:")
            val flow = client.stream(
                request = GenerationRequest.of(prompt = "Write a 100 word story on about Kotlin coroutines."),
            )
            flow.collect { chunk ->
                print(message = chunk.content)
            }
            println()
        })
    }
}

object OpenAiTypeStreamingExample {
    // choose what you want to demo
    private enum class Mode { NDJSON, JSON_FINAL }

    private val mode = Mode.NDJSON // <-- switch to JSON_FINAL to see the other path

    // ----- models -----
    @Serializable
    data class Person(val name: String, val age: Int)

    // used only for JSON_FINAL mode
    @Serializable
    data class PeopleEnvelope(val people: List<Person>)

    @JvmStatic
    fun main(
        args: Array<String>,
    ) = runBlocking {
        require(apiKey.isNotBlank()) { "OPENAI_API_KEY must be set" }

        val httpClient = ExampleHttpClient.createRecommendedHttpClient()
        val factory = LlmClientFactory(httpClient = httpClient, openAiApiKey = apiKey)

        when (mode) {
            Mode.NDJSON -> {
                // live, typed items as soon as they’re ready
                val client = factory.createOpenAiClient(
                    config = OpenAiConfig(
                        responseFormat = ResponseFormat.TEXT, // plain text
                        streamParsingMode = StreamParsingMode.NDJSON_PER_LINE, // parse per line
                        ndjsonDelimiter = "\n",
                        temperature = 0.7,
                        maxTokens = 800,
                    ),
                )

                val req = GenerationRequest.of(
                    prompt = """
                        You are a data generator.
                        Stream exactly 10 JSON objects, one per line (NDJSON).
                        Each line MUST be a single JSON object: {"name": string, "age": number}
                        Rules:
                        - Do NOT wrap in an array.
                        - Do NOT add any text or code fences.
                        - End each object with a newline.
                        - Ages 18–70, realistic names.
                        Begin immediately with the first JSON object.
                    """.trimIndent(),
                )

                println("Streaming NDJSON persons:")
                client.streamTyped(req, Person::class.java)
                    .mapNotNull { it.content } // keep only successfully parsed objects
                    .take(10) // stop after 10 items
                    .catch { e -> if (e !is CancellationException) throw e }
                    .collect { p -> println("→ ${p.name} (${p.age})") }
            }

            Mode.JSON_FINAL -> {
                // strict JSON payload at the end (schema-like). you still see raw tokens while streaming,
                // but you only get ONE typed object when the stream completes.
                val client = factory.createOpenAiClient(
                    config = OpenAiConfig(
                        responseFormat = ResponseFormat.JSON_OBJECT, // or JSON_SCHEMA if you expose it
                        streamParsingMode = StreamParsingMode.BUFFER_AND_PARSE_FINAL,
                        temperature = 0.2,
                        maxTokens = 1200,
                    ),
                )

                val req = GenerationRequest.of(
                    prompt = """
                        Produce a single JSON object of this exact shape:
                        {
                          "people": [
                            {"name": string, "age": number},  // total exactly 10 items
                            ...
                          ]
                        }
                        Constraints:
                        - Exactly 10 items in "people".
                        - No extra fields.
                        - Ages 18–70, realistic names.
                        Output only the JSON object, no prose or code fences.
                    """.trimIndent(),
                )

                println("Streaming (final JSON parsing at end):")
                val envelope = client.streamTyped(req, PeopleEnvelope::class.java)
                    .mapNotNull { it.content } // we only care about the final typed object
                    .last() // the producer emits it once at the end
                envelope.people.forEach { p ->
                    println("→ ${p.name} (${p.age})")
                }
            }
        }

        println()
    }
}
