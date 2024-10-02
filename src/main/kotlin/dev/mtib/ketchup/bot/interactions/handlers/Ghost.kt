package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.features.homeassistant.Client
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName

object Ghost : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PUBLIC
    override val name: String = "ghost"
    override val description: String = "Changes the color of the ghost"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.string("color", "The color of the ghost") {
            required = true
            kotlin.runCatching {
                Client.keepColors.forEach {
                    this.choice(it, it)
                }
            }
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()
        val color = event.interaction.getStringOptionByName("color")
        kotlin.runCatching {
            Client.setLight(color!!, 255, "light.ghost")
        }.fold(
            onSuccess = {
                response.respond {
                    content = "Ghost color changed to $color"
                }
            },
            onFailure = {
                response.respond {
                    content = "Failed to change ghost color: ${it.message}"
                }
            }
        )
    }
}