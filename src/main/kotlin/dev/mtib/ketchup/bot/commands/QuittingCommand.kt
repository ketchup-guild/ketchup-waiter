package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.storage.Storage.MagicWord
import dev.mtib.ketchup.bot.utils.getAnywhere

class QuittingCommand : Command(
    "quit",
    "Let us know that you quit",
    "Let's us know that you quit and will assign you updated roles",
) {
    override val category = Category.Role
    override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            val author = message.author
            if (author?.isBot != false) {
                return@on
            }
            val magicWord = getAnywhere<MagicWord>()
            if (message.content == "$magicWord $commandName") {
                message.reply {
                    content = ":tada: We are all so happy for you! (but sadly this command is still WIP)"
                }
            }
        }
    }
}