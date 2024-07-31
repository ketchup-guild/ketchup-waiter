package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.utils.isGod
import mu.KotlinLogging

class PingCommand() : Command(
    "ping",
    "Pings the bot",
    "Pings the bot and returns a pong message"
) {
    private val logger = KotlinLogging.logger { }
    override val completeness: Completeness = Completeness.Complete
    override val category: Category = Category.Admin

    override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            if (message.author?.isBot != false) {
                return@on
            }
            if (message.content == "$magicWord $name") {
                message.reply {
                    content = if (message.author.isGod) {
                        ":tada: Pong!"
                    } else {
                        "Pong!"
                    }
                }
            }
        }
    }
}