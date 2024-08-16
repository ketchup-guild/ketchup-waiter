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
import dev.mtib.ketchup.bot.interactions.helpers.Interactions.shouldIgnore
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getNumberOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName

object GamesBetween : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PUBLIC
    override val name: String = "games-between"
    override val description: String = "Lists games that can be played with a range of numbers of players requested"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.number("min", "The minimum number of players you want to play with") {
            required = true
        }
        it.number("max", "The maximum number of players you want to play with") {
            required = true
        }
        it.string("owner", "A regex to filter the owner by name")
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        if (event.shouldIgnore()) {
            return
        }
        val response = event.defer()

        val minNumberOfPlayers = event.interaction.getNumberOptionByName("min")?.toInt()
        val maxNumberOfPlayers = event.interaction.getNumberOptionByName("max")?.toInt()

        if (minNumberOfPlayers == null || maxNumberOfPlayers == null || minNumberOfPlayers > maxNumberOfPlayers) {
            response.respond {
                content = "Please provide a valid number of players"
            }
            return
        }

        val games = NotionClient.getGameForNumberOfPlayersBetween(minNumberOfPlayers, maxNumberOfPlayers)
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