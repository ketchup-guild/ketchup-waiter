package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAllAnywhere
import dev.mtib.ketchup.bot.utils.getAnywhere
import kotlinx.datetime.Clock

class StatsCommand : ChannelCommand(
    "stats",
    "Show bot stats",
    "Show bot stats",
) {
    override val category: Category = Category.Admin
    override val completeness: Completeness = Completeness.WIP

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        message.reply {
            content = buildString {
                val stats = mapOf(
                    "Ping" to kord.gateway.averagePing,
                    "Available JVM Memory" to "${
                        Runtime.getRuntime().freeMemory() / 1024 / 1024
                    }MB / ${Runtime.getRuntime().totalMemory() / 1024 / 1024}MB",
                    "Available Processors" to Runtime.getRuntime().availableProcessors(),
                    "Total Threads" to Thread.getAllStackTraces().keys.size,
                    "Max Concurrency" to kord.resources.maxConcurrency,
                    "Application Id" to kord.resources.applicationId,
                    "Magic Word" to getAnywhere<Storage.MagicWord>(),
                    "Flags" to getAnywhere<Storage.Flags>(),
                    "Command count" to getAllAnywhere<Command>().count(),
                    "Current time" to Clock.System.now(),
                )
                stats.forEach { (key, value) ->
                    appendLine("$key: $value")
                }
            }
        }
    }

    override suspend fun matchesSignature(kord: Kord, message: Message): Boolean {
        return message.content == prefix
    }
}