package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.KetchupBot.Companion.GOD_IDS
import dev.mtib.ketchup.bot.KetchupBot.Companion.MAGIC_WORD

class AboutCommand: Command(
    "about",
    "About the bot",
    "Shows information about the bot"
) {
    override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            if (message.author?.isBot != false) {
                return@on
            }
            if (message.content == "$MAGIC_WORD $commandName") {
                message.reply {
                    val user = kord.getUser(GOD_IDS.first())
                    content = buildString {
                        appendLine("Ketchup is a bot for the Ketchup server, written by ${user?.mention ?: "someone"}")
                        appendLine("Source code: https://github.com/ketchup-guild/ketchup-waiter")
                    }
                }
            }
        }
    }
}