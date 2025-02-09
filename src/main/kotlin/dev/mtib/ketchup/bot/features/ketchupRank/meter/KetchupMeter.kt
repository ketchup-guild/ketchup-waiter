package dev.mtib.ketchup.bot.features.ketchupRank.meter

import dev.mtib.ketchup.server.meter.MeterRegistry
import io.micrometer.core.instrument.Counter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object KetchupMeter {
    private val ketchupSpentCounters = mutableMapOf<String?, Counter>()
    private val mutex = Mutex()

    private fun getKetchupSpentCounter(username: String?): Counter {
        return ketchupSpentCounters.getOrPut(username) {
            if (username == null) {
                MeterRegistry.registry.counter("ketchup_spent_counter")
            } else {
                MeterRegistry.registry.counter("ketchup_spent_counter", "username", username)
            }
        }
    }

    suspend fun incrementKetchupSpentCounter(count: Int = 1, username: String? = null) {
        mutex.withLock {
            getKetchupSpentCounter(username).increment(count.toDouble())
        }
    }
}