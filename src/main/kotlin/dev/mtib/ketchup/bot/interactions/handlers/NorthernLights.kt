package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.mtib.ketchup.bot.features.northernlights.NorthernLightsDigest
import dev.mtib.ketchup.bot.features.northernlights.NorthernLightsDigest.RunResult.Aborted
import dev.mtib.ketchup.bot.features.northernlights.NorthernLightsDigest.RunResult.Posted
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.Visibility

object NorthernLights : Interaction {
    override val visibility: Visibility = Visibility.PRIVATE
    override val name: String = "northern-lights"
    override val description: String = "Run northern light job here"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.dmPermission = true
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()

        val data = NorthernLightsDigest.run(kord, event.interaction.channelId)

        response.respond {
            when (data) {
                is Posted -> {
                    content = "Northern Lights job completed: ${data.score} (sent ${data.messages.size} messages)"
                }

                is Aborted -> {
                    content = "Northern Lights job aborted: ${data.score}"
                }
            }
        }
    }
}