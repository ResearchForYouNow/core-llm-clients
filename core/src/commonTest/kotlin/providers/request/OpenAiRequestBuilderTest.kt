package providers.request

import io.github.researchforyounow.llm.providers.openai.config.OpenAiConfig
import io.github.researchforyounow.llm.providers.openai.config.Models
import io.github.researchforyounow.llm.providers.openai.config.ResponseFormat
import io.github.researchforyounow.llm.providers.openai.request.OpenAiRequestBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAiRequestBuilderTest {

    @Test
    fun `buildRequest uses defaults and sets messages`() {
        val cfg = OpenAiConfig.defaultConfig().copy(apiKey = "k", modelName = Models.GPT_4O)
        val builder = OpenAiRequestBuilder.create(cfg)
        val json = builder.buildRequest(systemMessage = "sys", prompt = "user")

        assertEquals(Models.GPT_4O, json["model"]?.toString()?.trim('"'))
        val messages = json["messages"]!!.toString()
        assertTrue(messages.contains("\"role\":\"system\""))
        assertTrue(messages.contains("\"content\":\"sys\""))
        assertTrue(messages.contains("\"role\":\"user\""))
        assertTrue(messages.contains("\"content\":\"user"))
        assertTrue(json["stream"]?.toString()?.toBooleanStrictOrNull() == (cfg.stream))
    }

    @Test
    fun `buildStreamRequest sets stream true`() {
        val cfg = OpenAiConfig.defaultConfig().copy(stream = false) // override default
        val builder = OpenAiRequestBuilder.create(cfg)
        val json = builder.buildStreamRequest(systemMessage = "s", prompt = "p")
        // stream must be true regardless of config.stream
        assertEquals("true", json["stream"].toString())
    }

    @Test
    fun `json_object responseFormat appends JSON instruction when missing`() {
        val cfg = OpenAiConfig.defaultConfig().copy(responseFormat = ResponseFormat.JSON_OBJECT)
        val builder = OpenAiRequestBuilder.create(cfg)
        val json = builder.buildRequest(systemMessage = "context", prompt = "answer plainly")
        val messages = json["messages"]!!.toString()
        assertTrue(messages.contains("Please provide your response in JSON format."))
    }

    @Test
    fun `respects stop sequences and logit bias`() {
        val cfg = OpenAiConfig.defaultConfig().copy(
            stopSequences = listOf("END", "STOP"),
            logitBias = mapOf("123" to 1.5, "456" to -0.5),
        )
        val builder = OpenAiRequestBuilder.create(cfg)
        val json = builder.buildRequest(systemMessage = "s", prompt = "p")
        val stop = json["stop"]!!.toString()
        assertTrue(stop.contains("END") && stop.contains("STOP"))
        val bias = json["logit_bias"]!!.toString()
        assertTrue(bias.contains("\"123\":1.5"))
        assertTrue(bias.contains("\"456\":-0.5"))
    }

    @Test
    fun `enableWebSearch omits temperature and adds web_search_options`() {
        val cfg = OpenAiConfig.defaultConfig().copy(
            enableWebSearch = true,
            modelName = Models.GPT_4O_MINI_SEARCH_PREVIEW,
            responseFormat = ResponseFormat.JSON_SCHEMA,
            jsonSchema = "{\"type\":\"object\",\"properties\":{\"ok\":{\"type\":\"boolean\"}},\"required\":[\"ok\"]}",
        )
        val builder = OpenAiRequestBuilder.create(cfg)
        val json = builder.buildRequest(systemMessage = "s", prompt = "p")
        assertEquals(json["temperature"], null)
        assertEquals(json["top_p"], null)
        assertEquals(json["frequency_penalty"], null)
        assertEquals(json["presence_penalty"], null)
        assertTrue(json["web_search_options"] != null)
    }

    @Test
    fun `json_schema includes name and schema wrapper`() {
        val cfg = OpenAiConfig.defaultConfig().copy(
            modelName = Models.GPT_4O_2024_08_06,
            responseFormat = ResponseFormat.JSON_SCHEMA,
            jsonSchema = "{\"type\":\"object\",\"properties\":{\"ok\":{\"type\":\"boolean\"}},\"required\":[\"ok\"]}",
            jsonSchemaName = "result",
        )
        val builder = OpenAiRequestBuilder.create(cfg)
        val json = builder.buildRequest(systemMessage = "s", prompt = "p")
        val responseFormat = json["response_format"]!!.toString()
        assertTrue(responseFormat.contains("\"type\":\"json_schema\""))
        assertTrue(responseFormat.contains("\"name\":\"result\""))
        assertTrue(responseFormat.contains("\"schema\""))
    }

    @Test
    fun `json_schema uses wrapper when already provided`() {
        val cfg = OpenAiConfig.defaultConfig().copy(
            modelName = Models.GPT_4O_2024_08_06,
            responseFormat = ResponseFormat.JSON_SCHEMA,
            jsonSchema = "{\"name\":\"wrapped\",\"schema\":{\"type\":\"object\"}}",
            jsonSchemaName = "ignored",
        )
        val builder = OpenAiRequestBuilder.create(cfg)
        val json = builder.buildRequest(systemMessage = "s", prompt = "p")
        val responseFormat = json["response_format"]!!.toString()
        assertTrue(responseFormat.contains("\"name\":\"wrapped\""))
        assertTrue(responseFormat.contains("\"schema\":{\"type\":\"object\"}"))
        assertTrue(!responseFormat.contains("\"schema\":{\"name\""))
    }

    @Test
    fun `json_schema with name and schema non-wrapper is still wrapped`() {
        val cfg = OpenAiConfig.defaultConfig().copy(
            modelName = Models.GPT_4O_2024_08_06,
            responseFormat = ResponseFormat.JSON_SCHEMA,
            jsonSchema = "{\"name\":123,\"schema\":\"string\"}",
            jsonSchemaName = "result",
        )
        val builder = OpenAiRequestBuilder.create(cfg)
        val json = builder.buildRequest(systemMessage = "s", prompt = "p")
        val responseFormat = json["response_format"]!!.toString()
        assertTrue(responseFormat.contains("\"name\":\"result\""))
        assertTrue(responseFormat.contains("\"schema\""))
    }
}
