package io.github.researchforyounow.llm.client

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.random.Random

/**
 * Simple retry policy supporting exponential backoff with jitter.
 *
 * @property maxAttempts total number of attempts including the initial one
 * @property initialDelayMillis delay before the first retry in milliseconds
 * @property maxDelayMillis maximum delay between retries
 * @property jitterMillis random additional delay (0..jitterMillis) added to each retry delay
 */
data class RetryPolicy(
    val maxAttempts: Int = 1,
    val initialDelayMillis: Long = 100,
    val maxDelayMillis: Long = 1_000,
    val jitterMillis: Long = 100,
) {
    companion object {
        /** Policy that disables retries. */
        val NO_RETRY = RetryPolicy(1, 0, 0, 0)
    }
}

/**
 * Executes [block] respecting this [RetryPolicy].
 * Retries are only performed when [maxAttempts] > 1.
 */
suspend fun <T> RetryPolicy.execute(
    block: suspend () -> T,
): T {
    var attempt = 0
    var delayMillis = initialDelayMillis
    while (true) {
        try {
            return block()
        } catch (e: Exception) {
            attempt++
            if (attempt >= maxAttempts) throw e
        }
        val jitter = if (jitterMillis > 0) Random.nextLong(jitterMillis) else 0
        delay(delayMillis + jitter)
        delayMillis = min(delayMillis * 2, maxDelayMillis)
    }
}
