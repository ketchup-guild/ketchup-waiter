package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.mtib.ketchup.bot.commands.LeaveCommand.Companion.removeUser
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.Visibility
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.Visibility.PRIVATE

object Leave : Interaction {
    override val name: String = "leave"
    override val description: String = "Leave the channel"

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()
        val author = event.interaction.user
        val channel = event.interaction.channel.asChannelOf<TextChannel>()
        removeUser(channel, author)
        response.respond { content = "Removing you" }
    }

    override val visibility: Visibility = PRIVATE
}