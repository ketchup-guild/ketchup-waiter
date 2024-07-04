package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.errors.UnauthorizedException
import dev.mtib.ketchup.bot.utils.isGod

abstract class AdminCommand(
    commandName: String,
    commandShortDescription: String,
    commandHelp: String,
) : Command(
    commandName,
    commandShortDescription,
    commandHelp,
) {
    val adminOnly = true
    override val category = Category.Admin

    private suspend fun MessageCreateEvent.checkAdmin() {
        if (!message.author.isGod) {
            message.reply {
                content = "You are not allowed to use this command"
            }
            throw UnauthorizedException(message.author, this@AdminCommand)
        }
    }

    override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            if (message.author?.isBot != false) {
                return@on
            }
            if (matchesSignature(kord, message)) {
                try {
                    checkAdmin()
                } catch (e: UnauthorizedException) {
                    return@on
                }
                authorized(kord)
            }
        }
    }

    abstract suspend fun MessageCreateEvent.authorized(kord: Kord)
}