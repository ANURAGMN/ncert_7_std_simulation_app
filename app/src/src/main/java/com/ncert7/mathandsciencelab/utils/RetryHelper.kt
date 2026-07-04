package com.ncert7.mathandsciencelab.utils

import com.ncert7.mathandsciencelab.debug.DebugLogger
import kotlinx.coroutines.delay

/**
 * Retry helper utility for handling database load failures with exponential backoff.
 * Automatically retries failed database operations with a configurable number of attempts.
 */
object RetryHelper {

    private const val TAG = "RetryHelper"

    /**
     * Execute a suspend function with automatic retry logic.
     * Uses exponential backoff between retries.
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 100L,
        backoffMultiplier: Double = 2.0,
        functionName: String = "unknown",
        block: suspend () -> T?
    ): T? {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        var result: T? = null
        var success = false

        for (attempt in 0..maxRetries) {
            try {
                DebugLogger.debugLog(TAG, "Attempt ${attempt + 1}/${maxRetries + 1} for $functionName")
                result = block()

                if (result != null) {
                    DebugLogger.debugLog(TAG, "Successfully loaded data from $functionName on attempt ${attempt + 1}")
                    success = true
                    break
                } else if (attempt < maxRetries) {
                    DebugLogger.debugLog(TAG, "Null result from $functionName, retrying after ${currentDelay}ms")
                    delay(currentDelay)
                    currentDelay = (currentDelay * backoffMultiplier).toLong()
                } else {
                    DebugLogger.debugLog(TAG, "All retries exhausted for $functionName, returning null")
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    DebugLogger.debugLog(
                        TAG,
                        "Error loading from $functionName (attempt ${attempt + 1}): ${e.message}, retrying after ${currentDelay}ms"
                    )
                    delay(currentDelay)
                    currentDelay = (currentDelay * backoffMultiplier).toLong()
                } else {
                    DebugLogger.debugLog(
                        TAG,
                        "All retries exhausted for $functionName after error: ${e.message}"
                    )
                }
            }
        }

        if (!success) {
            DebugLogger.debugLog(TAG, "RetryHelper: Failed to load $functionName. Last error: ${lastException?.message}")
        }

        return result
    }

    /**
     * Execute a suspend function that returns a list with automatic retry logic.
     */
    suspend fun <T> executeWithRetryList(
        maxRetries: Int = 3,
        initialDelayMs: Long = 100L,
        functionName: String = "unknown",
        block: suspend () -> List<T>
    ): List<T> {
        val result = executeWithRetry(
            maxRetries = maxRetries,
            initialDelayMs = initialDelayMs,
            functionName = functionName,
            block = {
                val data = block()
                data.ifEmpty { null }
            }
        )
        return result ?: emptyList()
    }
}
