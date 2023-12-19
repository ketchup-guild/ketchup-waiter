package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.storage.Storage.Gods
import dev.mtib.ketchup.bot.storage.Storage.MagicWord
import dev.mtib.ketchup.bot.utils.getAnywhere

class AboutCommand : Command(
    "about",
    "About the bot",
    "Shows information about the bot"
) {
    override suspend fun register(kord: Kord) {
        suspend fun handleMessage(author: User?, message: Message) {
            if (author?.isBot != false) {
                return
            }
            val magicWord = getAnywhere<MagicWord>()
            if (message.content == "$magicWord $commandName") {
                message.reply {
                    val gods = getAnywhere<Gods>().asList()
                    val user = kord.getUser(gods.first())
                    content = buildString {
                        appendLine("Ketchup is a bot for the Ketchup server, written by ${user?.mention ?: "someone"}")
                        appendLine("Source code: https://github.com/ketchup-guild/ketchup-waiter")
                    }
                }
            }
        }
        kord.on<MessageCreateEvent> {
            handleMessage(message.author, message)
        }
    }
}