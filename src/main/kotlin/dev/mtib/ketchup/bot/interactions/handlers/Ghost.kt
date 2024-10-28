package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.*
import dev.mtib.ketchup.bot.features.homeassistant.Client
import dev.mtib.ketchup.bot.features.homeassistant.Client.ColorHex.Companion.toColorHex
import dev.mtib.ketchup.bot.features.homeassistant.Client.ColorName.Companion.asColorName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getBooleanOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getDoubleOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.onSubcommand
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.minutes

object Ghost : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PUBLIC
    override val name: String = "ghost"
    override val description: String = "Changes the color of the ghost"

    private var lastFoggerJob: Job? = null

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.subCommand("color", "Change the color of the ghost") {
            string("color", "The color of the ghost") {
                required = true
                kotlin.runCatching {
                    Client.keepColors.forEach {
                        this.choice(it, it)
                    }
                }
            }
            number("brightness", "The brightness of the ghost") {
                required = false
                minValue = 0.0
                maxValue = 255.0
            }
            boolean("fog", "Set fog machine on or off") {
                required = false
            }
        }
        it.subCommand("hex", "Change the color of the ghost using hex") {
            string("hex_code", "The hex color of the ghost (starting # is optional)") {
                required = true
            }
            boolean("fog", "Set fog machine on or off") {
                required = false
            }
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()

        event.interaction.onSubcommand("color") {
            val color = event.interaction.getStringOptionByName("color")!!
            val brightness = event.interaction.getDoubleOptionByName("brightness")?.toInt() ?: 255
            val fog = event.interaction.getBooleanOptionByName("fog") ?: false
            kotlin.runCatching {
                Client.setLight(color.asColorName(), brightness, "light.ghost")
                Client.setLight(fog, "light.ghost_fogger")
                if (fog) {
                    setFoggerTimeout()
                }
            }.fold(
                onSuccess = {
                    response.respond {
                        content =
                            "Ghost color changed to $color ($brightness/255 brightness) with fog turned ${if (fog) "on" else "off"}"
                    }
                },
                onFailure = {
                    response.respond {
                        content = "Failed to change ghost color: ${it.message}"
                    }
                }
            )
        }

        event.interaction.onSubcommand("hex") {
            val hex = event.interaction.getStringOptionByName("hex_code")!!
            val fog = event.interaction.getBooleanOptionByName("fog") ?: false
            kotlin.runCatching {
                val color = hex.toColorHex()
                Client.setLight(color, color.brightness, "light.ghost")
                Client.setLight(fog, "light.ghost_fogger")
                if (fog) {
                    setFoggerTimeout()
                }
                color
            }.fold(
                onSuccess = {
                    response.respond {
                        content = "Ghost color changed to $it with fog turned ${if (fog) "on" else "off"}"
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

    private fun setFoggerTimeout() {
        synchronized(Ghost) {
            lastFoggerJob?.cancel()
            lastFoggerJob = CoroutineScope(Dispatchers.Default).launch {
                delay(10.minutes)
                synchronized(Ghost) {
                    Client.setLight(false, "light.ghost_fogger")
                    lastFoggerJob = null
                }
            }
        }
    }
}