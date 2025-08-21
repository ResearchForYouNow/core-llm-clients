# Streaming API

This document outlines the streaming API for the library. The API streams partial model responses using Kotlin Flow.

## Proposed Function Signature

```kotlin
fun stream(request: GenerationRequest): Flow<StreamChunk>
```

- `GenerationRequest` carries the prompt and request metadata.
- `Flow<StreamChunk>` emits partial model responses as they become available.

## Cancellation

Consumers may cancel collection of the returned `Flow`. Cancellation should propagate to the underlying HTTP request so the provider stops work immediately.

## Backpressure

`Flow` provides natural backpressure. Chunks are produced as the consumer collects them. Implementations may expose buffering options but should avoid unbounded memory usage.

## Future Work / TODOs

- Add examples and contract tests.
- Provide configuration for stream timeouts and buffer sizes.
- Integrate usage metrics and error handling for streaming.

