package dev.mtib.ketchup.server.auth

import dev.mtib.ketchup.server.meter.MeterRegistry

object AuthMeter {
    private val authTokenCheckCounter = MeterRegistry.registry.counter("auth_token_check_counter")

    fun incrementAuthTokenCounter(count: Double = 1.0) {
        authTokenCheckCounter.increment(count)
    }
}