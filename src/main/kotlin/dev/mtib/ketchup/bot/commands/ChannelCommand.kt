package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
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
                try {
                    handleMessage(message.author!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                    message.reply {
                        content =
                            "An unexpected error occurred while processing the command. Please try again later or forward " +
                                    "this message to the bot developer:\n```${e.message}```"
                    }
                    message.reply {
                        content = "```${e.stackTraceToString().let { it.substring(0..minOf(1990, it.length - 1)) }}```"
                    }
                }
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