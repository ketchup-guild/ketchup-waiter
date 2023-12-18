package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.KetchupBot
import dev.mtib.ketchup.bot.utils.isGod
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun Kord.registerPingListener() {
    logger.debug { "Registering ping listener" }
    on<MessageCreateEvent> {
        if (message.content == "${KetchupBot.MAGIC_WORD} ping") {
            if (message.author.isGod) {
                message.channel.createMessage(":tada: Pong!")
            } else {
                message.channel.createMessage("Pong!")
            }
        }
    }
    logger.info { "Registered ping listener" }
}
