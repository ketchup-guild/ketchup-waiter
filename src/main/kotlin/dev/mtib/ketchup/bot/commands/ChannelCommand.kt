package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.utils.matchesSignature

abstract class ChannelCommand(
    commandName: String,
    commandShortDescription: String,
    commandHelp: String,
) : Command(
    commandName,
    commandShortDescription,
    commandHelp,
) {
    final override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            if (message.matchesSignature(this@ChannelCommand)) {
                handleMessage(message.author!!)
            }
        }
        kord.on<ReactionAddEvent> {
            handleReaction()
        }
        kord.on<ReactionRemoveEvent> {
            handleReaction()
        }
    }

    open suspend fun MessageCreateEvent.handleMessage(author: User) {

    }

    open suspend fun ReactionAddEvent.handleReaction() {

    }

    open suspend fun ReactionRemoveEvent.handleReaction() {

    }
}