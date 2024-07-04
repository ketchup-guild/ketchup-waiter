package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on

class QuittingCommand : Command(
    "quit",
    "Let us know that you quit Colourbox",
    "Let's us know that you quit and will assign you updated roles",
) {
    override val category = Category.Role
    override val completeness = Completeness.Stubbed
    override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            val author = message.author
            if (author?.isBot != false) {
                return@on
            }
            if (message.content == "$magicWord $commandName") {
                message.reply {
                    content = ":tada: We are all so happy for you! (but sadly this command is still WIP)"
                }
            }
        }
    }
}