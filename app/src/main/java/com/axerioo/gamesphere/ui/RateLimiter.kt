package com.axerioo.gamesphere.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// --- RateLimiter Object ---
// A simple object to limit the rate of execution of a given block of code (API calls).
// This implementation aims to ensure that no more than `MAX_REQUESTS_PER_SECOND` are made within any one-second window.

object RateLimiter {
    private const val MAX_REQUESTS_PER_SECOND = 4
    private const val ONE_SECOND_MS = 1000L

    private val mutex = Mutex()
    private val requestTimestamps = mutableListOf<Long>()

    // --- Execute Function ---
    // Executes the provided suspendable `block` of code, ensuring rate limits are respected.
    // @param block: The suspendable lambda function to execute.
    // @return The result of the executed `block`.

    suspend fun <T> execute(block: suspend () -> T): T {
        mutex.withLock {
            val currentTime = System.currentTimeMillis()

            // Remove any timestamps from `requestTimestamps` that are older than one second from the current time.
            requestTimestamps.removeAll { timestamp -> currentTime - timestamp > ONE_SECOND_MS }

            // If the number of requests made in the last second has reached or exceeded the maximum allowed, then wait.
            if (requestTimestamps.size >= MAX_REQUESTS_PER_SECOND) {
                val oldestRequestTimeInWindow = requestTimestamps.firstOrNull() ?: currentTime
                val timeToWait = ONE_SECOND_MS - (currentTime - oldestRequestTimeInWindow)

                if (timeToWait > 0) {
                    delay(timeToWait) // Suspend the coroutine for the calculated duration.
                }

                val newCurrentTimeAfterWait = System.currentTimeMillis()
                requestTimestamps.removeAll { timestamp -> newCurrentTimeAfterWait - timestamp > ONE_SECOND_MS }
            }
            // Add the timestamp of the current request to the list.
            requestTimestamps.add(System.currentTimeMillis())
        }
        return block()
    }
}