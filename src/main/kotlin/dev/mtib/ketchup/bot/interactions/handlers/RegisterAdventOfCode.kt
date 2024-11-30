package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getLongOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import dev.mtib.ketchup.bot.features.aoc.Client as AocClient

object RegisterAdventOfCode : Interaction {
    override val visibility: Interaction.Companion.Visibility
        get() = Interaction.Companion.Visibility.PRIVATE
    override val name: String
        get() = "register_advent_of_code"
    override val description: String
        get() = "Register a channel to receive daily Advent of Code leaderboards"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        super.build(it)
        it.integer("year", "The year of the Advent of Code event")
        it.integer("leaderboard", "The owner ID of the Advent of Code leaderboard")
        it.string("cookie", "The cookie to access the Advent of Code leaderboard")
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val r = event.defer()

        val year = event.interaction.getLongOptionByName("year")!!.toString()
        val leaderboard = event.interaction.getLongOptionByName("leaderboard")!!
        val cookie = event.interaction.getStringOptionByName("cookie")!!

        AocClient.addListener(
            snowflake = event.interaction.channelId.value.toString(),
            event = year,
            ownerId = leaderboard,
            cookie = cookie
        )

        r.respond { content = "Registered Advent of Code" }
    }
}