package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.features.notion.NotionClient
import dev.mtib.ketchup.bot.features.notion.models.NotionGame.Companion.filterByOwnerRegex
import dev.mtib.ketchup.bot.features.notion.models.NotionGame.Companion.toOwnerMarkdown
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName

object GamesAll : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PUBLIC
    override val name: String = "games-all"
    override val description: String = "Lists all games available in the database"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.string("owner", "A regex to filter the owner by name")
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()

        val games = NotionClient.getAllGames().let {
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