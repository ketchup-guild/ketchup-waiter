package dev.mtib.ketchup.bot.features.meter

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.server.meter.MeterRegistry
import io.micrometer.core.instrument.Counter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object DiscordMeter : Feature {
    private val mutex = Mutex()
    private val reactionMeters = mutableMapOf<String, Counter>()
    private val messageMeters = mutableMapOf<String, Counter>()

    private suspend fun incrementReactionCounter(username: String, reaction: String, count: Int = 1) {
        mutex.withLock {
            reactionMeters.getOrPut(username) {
                MeterRegistry.registry.counter("reaction_counter", "username", username, "reaction", reaction)
            }.increment(count.toDouble())
        }
    }

    private suspend fun incrementMessageCounter(username: String, count: Int = 1) {
        mutex.withLock {
            messageMeters.getOrPut(username) {
                MeterRegistry.registry.counter("message_counter", "username", username)
            }.increment(count.toDouble())
        }
    }

    override fun register(kord: Kord) {
        kord.on<ReactionAddEvent> {
            val username = user.asUser().username
            incrementReactionCounter(username, this.emoji.name)
        }
        kord.on<MessageCreateEvent> {
            val username = message.author?.username ?: return@on
            incrementMessageCounter(username)
        }
    }

}