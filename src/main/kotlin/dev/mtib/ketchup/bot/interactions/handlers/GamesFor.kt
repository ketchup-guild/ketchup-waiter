package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.number
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.features.notion.NotionClient
import dev.mtib.ketchup.bot.features.notion.models.NotionGame.Companion.filterByOwnerRegex
import dev.mtib.ketchup.bot.features.notion.models.NotionGame.Companion.toOwnerMarkdown
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getNumberOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName

object GamesFor : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PUBLIC
    override val name: String = "games-for"
    override val description: String = "Lists games that can be played with the number of players requested"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.number("players", "The number of players you want to play with") {
            required = true
        }
        it.string("owner", "A regex to filter the owner by name") {
            required = false
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()

        val numberOfPlayers = event.interaction.getNumberOptionByName("players")?.toInt()

        if (numberOfPlayers == null) {
            response.respond {
                content = "Please provide a valid number of players"
            }
            return
        }

        val games = NotionClient.getGameForNumberOfPlayersEquals(numberOfPlayers)
            .let {
                val ownerRegex = event.interaction.getStringOptionByName("owner")?.toRegex()
                if (ownerRegex != null) {
                    it.filterByOwnerRegex(ownerRegex)
                } else {
                    it
                }
            }

        response.respond {
            content =
                games.toOwnerMarkdown()
                    .let { if (it.length > 2000) it.substring(0, 1997) + "..." else it }
        }
    }
}