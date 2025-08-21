package request

/**
 * Configuration class for generation requests.
 * This class encapsulates all the parameters needed for a generation request,
 * simplifying the method signatures and making the API more maintainable.
 */
data class GenerationRequest(
    /**
     * The user prompt or request. If systemMessage is provided, this is used as the user message.
     * If systemMessage is null or empty, this is used as the complete instruction.
     */
    val prompt: String,
    /**
     * Optional system message that provides context and instructions to the LLM.
     * When provided, it will be combined with the prompt according to the provider's requirements.
     */
    val systemMessage: String? = null,
    /**
     * Optional idempotency key to make safe retries idempotent at the provider.
     * When provided, providers that support idempotency will receive this key via headers.
     */
    val idempotencyKey: String? = null,
    /**
     * Optional request tags for logging/metrics correlation.
     * Will be passed via headers as a compact string (e.g., key1=value1;key2=value2).
     */
    val tags: Map<String, String>? = null,
) {
    /**
     * Builder class for GenerationRequest.
     * Provides a fluent interface for building GenerationRequest instances.
     */
    class Builder {
        private var prompt: String = ""
        private var systemMessage: String? = null
        private var idempotencyKey: String? = null
        private var tags: Map<String, String>? = null

        /**
         * Sets the user prompt or request.
         */
        fun prompt(
            prompt: String,
        ) = apply { this.prompt = prompt }

        /**
         * Sets the optional system message.
         */
        fun systemMessage(
            systemMessage: String?,
        ) = apply { this.systemMessage = systemMessage }

        /**
         * Sets the optional idempotency key.
         */
        fun idempotencyKey(
            idempotencyKey: String?,
        ) = apply { this.idempotencyKey = idempotencyKey }

        /**
         * Sets optional request tags.
         */
        fun tags(
            tags: Map<String, String>?,
        ) = apply { this.tags = tags }

        /**
         * Builds the GenerationRequest instance.
         * @throws IllegalStateException if prompt is empty
         */
        fun build(): GenerationRequest {
            if (prompt.isEmpty()) {
                throw IllegalStateException("Prompt cannot be empty")
            }

            return GenerationRequest(
                prompt = prompt,
                systemMessage = systemMessage,
                idempotencyKey = idempotencyKey,
                tags = tags,
            )
        }
    }

    companion object {
        /**
         * Creates a new builder for GenerationRequest.
         */
        fun builder(): Builder = Builder()

        /**
         * Creates a simple GenerationRequest with just a prompt.
         */
        fun of(
            prompt: String,
        ): GenerationRequest = GenerationRequest(prompt = prompt)

        /**
         * Creates a GenerationRequest with both prompt and system message.
         */
        fun of(
            prompt: String,
            systemMessage: String,
        ): GenerationRequest {
            return GenerationRequest(
                prompt = prompt,
                systemMessage = systemMessage,
            )
        }
    }
}
