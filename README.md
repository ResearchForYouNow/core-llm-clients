# LLM Clients

A Kotlin library for interacting with various Large Language Model (LLM) providers through a unified interface. Supports OpenAI and Google Gemini with a simple, consumer-friendly API.

## Features

- **Unified Interface**: Single API for multiple LLM providers (OpenAI, Gemini)
- **Security First**: No hardcoded API keys - consumers must provide their own keys explicitly
- **Explicit HTTP Client**: Consumers must provide and pass their own HttpClient instance (no library-provided HTTP client)
- **Type-Safe**: Kotlin serialization for structured request/response handling
- **Production Ready**: Built-in timeouts, connection pooling, and error handling
- **Factory Pattern**: Instantiate once with API keys and HttpClient; create provider clients without repeating secrets
- **Extensible**: Clean architecture for adding new providers
- **Streaming**: Stream partial responses via Kotlin [Flow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/); design details in [docs/STREAMING_API_DESIGN.md](docs/STREAMING_API_DESIGN.md)

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.researchforyounow:llm-clients:0.7.0")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.researchforyounow</groupId>
    <artifactId>llm-clients</artifactId>
    <version>0.7.0</version>
</dependency>
```

## Quick Start

### 1. Get Your API Keys

**SECURITY NOTICE**: This library requires you to provide your own API keys explicitly. No hardcoded API keys are included for security reasons.

- **OpenAI API Key**: Get from [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)
- **Gemini API Key**: Get from [https://makersuite.google.com/app/apikey](https://makersuite.google.com/app/apikey)

### 2. Initialize Library

```kotlin
// Call once at application startup with your API keys
fun initializeLibrary(): LlmClientFactory {
    val httpClient = ExampleHttpClient.createRecommendedHttpClient()
    return LlmClientFactory(
        httpClient = httpClient,
        openAiApiKey = System.getenv("OPENAI_API_KEY") ?: "your-openai-api-key-here",
        geminiApiKey = System.getenv("GEMINI_API_KEY") ?: "your-gemini-api-key-here"
    )
}
```
### 3. Use Clients Anywhere

Note: This library does not use keys.properties. Configure API keys via environment variables or your secrets manager.
The factory holds your keys and shared HttpClient so callers only supply model options.

```kotlin
val factory = initializeLibrary()

val openAiClient = factory.createOpenAiClient(
    config = OpenAiConfig(
        model = OpenAiModel.GPT_4_TURBO_2024_04_09,
        temperature = 0.3
    )
)

val geminiClient = factory.createGeminiClient(
    config = GeminiConfig(
        model = GeminiModel.GEMINI_1_5_FLASH_LATEST
    )
)

// Make a structured request
val structuredResult = openAiClient.generate(
    request = GenerationRequest.of(
        "What is the capital of Japan?",
        "You are a helpful assistant."
    ),
    responseType = MyDataClass::class.java
)

// Or get plain text easily
val textResult = openAiClient.generateText(
    GenerationRequest.of("Tell me a short joke about Kotlin")
)

structuredResult.fold(
    onSuccess = { response -> println("Success: $response") },
    onFailure = { error -> println("Error: ${error.message}") }
)
```

## Complete Example

For a comprehensive, production-ready example showing:
- One-time library initialization
- Multiple client configurations
- Error handling and best practices
- Real-world usage patterns

**See: [`ConsumerReadyExample.kt`](examples/src/main/kotlin/examples)**

## Supported Models

### OpenAI
- GPT-4 Turbo (default: `gpt-4-turbo-2024-04-09`)
- GPT-3.5 Turbo
- Custom models via configuration

### Google Gemini
- Gemini 1.5 Flash (default: `gemini-1.5-flash-latest`)
- Gemini 1.5 Pro
- Custom models via configuration

## Configuration Options

### OpenAI Client
```kotlin
val client = factory.createOpenAiClient(
    config = OpenAiConfig(
        model = OpenAiModel.GPT_4_TURBO_2024_04_09,  // Optional
        temperature = 0.7,                           // Optional (0.0-2.0)
        maxTokens = 2000                             // Optional
    ),
)
```

### Gemini Client
```kotlin
val client = factory.createGeminiClient(
    config = GeminiConfig(
        model = GeminiModel.GEMINI_1_5_FLASH_LATEST   // Optional
    ),
)
```

### Retry Policy

Both `OpenAiConfig` and `GeminiConfig` accept an optional `retryPolicy` parameter.
Retries use exponential backoff with jitter and are only applied when an `idempotencyKey`
is supplied in the `GenerationRequest`.

```kotlin
val policy = RetryPolicy(maxAttempts = 3, initialDelayMillis = 200, jitterMillis = 100)
val client = factory.createOpenAiClient(
    config = OpenAiConfig(retryPolicy = policy)
)
```

## Advanced Usage

### Custom Response Types
```kotlin
@Serializable
data class StructuredResponse(
    val answer: String,
    val confidence: Double,
    val reasoning: String
)

val result = client.generate(
    request = GenerationRequest.of(
        "Explain photosynthesis",
        "Respond in JSON format with answer, confidence, and reasoning fields."
    ),
    responseType = StructuredResponse::class.java
)
```

### Multiple Configurations
```kotlin
// Different clients for different use cases
val creativeClient = factory.createOpenAiClient(
    config = OpenAiConfig(temperature = 0.9)
)
val factualClient = factory.createOpenAiClient(
    config = OpenAiConfig(temperature = 0.1)
)
```

### Usage Metrics Hook

Both OpenAI and Gemini can report token usage information. Supply a `usageSink`
in the configuration to observe normalized metrics:

```kotlin
val client = factory.createOpenAiClient(
    config = OpenAiConfig(
        usageSink = { usage ->
            println("prompt=${usage.promptTokens} completion=${usage.completionTokens}")
        }
    )
)
```

The `LlmUsage` model normalizes provider-specific fields (e.g., OpenAI
`prompt_tokens`/`completion_tokens`). Providers that don't return usage simply
never invoke the sink.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## API Stability and Internal Types

This library follows a small, stable public API surface centered on the LlmClient
interfaces and the request/response models shown in the examples. Implementation
details that live in core-api.

## Provider Support Matrix

| Provider      | Generate (sync) | Streaming (Flow) | Images | Structured JSON parsing                | Retry policy                       | Usage metrics (LlmUsageSink) | Error mapping (LlmError) | Notes                                  |
|---------------|-----------------|------------------|--------|----------------------------------------|------------------------------------|------------------------------|--------------------------|----------------------------------------|
| OpenAI        | Yes             | Yes              | Yes    | Yes (native response_format supported) | Yes (exponential backoff + jitter) | Yes                          | Yes                      | Default model: gpt-4-turbo-2024-04-09  |
| Google Gemini | Yes             | Yes              | No     | Yes (prompt-guided parsing)            | Yes (exponential backoff + jitter) | Yes                          | Yes                      | Default model: gemini-1.5-flash-latest |

## Image Generation (OpenAI)

- Generate images using OpenAI Images API via OpenAiClient.generateImage.
- Returns either URLs or base64-encoded JSON depending on ImageResponseFormat.
- Models and sizes are constrained by OpenAI (e.g., DALL·E 3 supports 1024x1024, 1024x1792, 1792x1024 with n=1).
- See full example: examples/src/main/kotlin/examples/OpenAiImageExample.kt

Example:
```kotlin
val client = factory.createOpenAiClient(OpenAiConfig.defaultConfig())
val imgReq = ImageGenerationRequest(
    prompt = "A watercolor painting of a mountain at sunrise",
    n = 1,
    size = "1024x1024",
    responseFormat = ImageResponseFormat.URL,
    model = OpenAiImageModel.DALL_E_3
)
val result = client.generateImage(imgReq)
result.onSuccess { images ->
    images.forEach { println(it.url ?: "[base64 image]") }
}
```

## Configuration Reference

The LlmClientFactory injects API keys from your environment or secrets manager (see Quick Start). You configure per-provider behavior via config objects when creating clients.

### OpenAI configuration (OpenAiConfig)

| Parameter        | Type                | Default                                    | Notes                                                |
|------------------|---------------------|--------------------------------------------|------------------------------------------------------|
| model            | OpenAiModel         | OpenAiModel.GPT_4_TURBO_2024_04_09         | Use enum. API model id available as model.modelName. |
| temperature      | Double              | 0.28                                       | Range 0.0..2.0.                                      |
| maxTokens        | Int                 | 4000                                       | Must be > 0.                                         |
| topP             | Double              | 1.0                                        | Range 0.0..1.0.                                      |
| frequencyPenalty | Double              | 0.0                                        | Range -2.0..2.0.                                     |
| presencePenalty  | Double              | 0.0                                        | Range -2.0..2.0.                                     |
| stopSequences    | List<String>        | []                                         | Up to 4 sequences.                                   |
| seed             | Int?                | null                                       | Optional deterministic seed.                         |
| responseFormat   | ResponseFormat      | JSON_OBJECT                                | TEXT, JSON_OBJECT, or JSON_SCHEMA.                   |
| jsonSchema       | String?             | null                                       | Used when responseFormat=JSON_SCHEMA.                |
| user             | String?             | null                                       | Optional user identifier for OpenAI.                 |
| logitBias        | Map<String, Double> | {}                                         | Up to 300 entries, values -100..100.                 |
| stream           | Boolean             | false                                      | If true, enables streaming on request.               |
| apiUrl           | String              | https://api.openai.com/v1/chat/completions | Base URL for Chat Completions.                       |
| retryPolicy      | RetryPolicy         | NO_RETRY                                   | Exponential backoff with jitter when set.            |
| usageSink        | LlmUsageSink?       | null                                       | Callback for normalized usage metrics.               |
| apiKey           | String              | ""                                         | Injected by LlmClientFactory (e.g., OPENAI_API_KEY). |

### Gemini configuration (GeminiConfig)

| Parameter       | Type          | Default                          | Notes                                                                                                        |
|-----------------|---------------|----------------------------------|--------------------------------------------------------------------------------------------------------------|
| model          | GeminiModel   | GeminiModel.GEMINI_1_5_FLASH_LATEST | Use enum. API model id available as model.modelName.                                                         |
| temperature     | Double        | 0.7                     | Range 0.0..2.0.                                                                                              |
| topK            | Int           | 40                      | Must be > 0.                                                                                                 |
| topP            | Double        | 0.95                    | Range 0.0..1.0.                                                                                              |
| maxOutputTokens | Int           | 2048                    | Must be > 0.                                                                                                 |
| candidateCount  | Int           | 1                       | Number of candidates to return.                                                                              |
| stopSequences   | List<String>  | []                      | Optional stop sequences.                                                                                     |
| apiUrl          | String        | auto                    | When empty, computed as https://generativelanguage.googleapis.com/v1beta/models/{model.modelName}:generateContent. |
| retryPolicy     | RetryPolicy   | NO_RETRY                | Exponential backoff with jitter when set.                                                                    |
| usageSink       | LlmUsageSink? | null                    | Callback for normalized usage metrics.                                                                       |
| apiKey          | String        | ""                      | Injected by LlmClientFactory (e.g., GEMINI_API_KEY).                                                         |

### Environment variables (suggested)

- OPENAI_API_KEY: OpenAI secret used by LlmClientFactory.
- GEMINI_API_KEY: Gemini secret used by LlmClientFactory.

See examples in examples/ for usage, including streaming and structured responses.
