package dev.mtib.ketchup.bot.interactions.interfaces

import dev.kord.common.entity.optional.firstOrNull
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.entity.interaction.ActionInteraction
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder

interface Interaction {
    val visibility: Visibility
    val name: String
    val description: String

    suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord): Unit
    suspend fun build(it: GlobalChatInputCreateBuilder): Unit {}

    companion object {
        enum class Visibility {
            PUBLIC,
            PRIVATE
        }

        fun ActionInteraction.getStringOptionByName(name: String): String? {
            return this.data.data.options.firstOrNull { it.name == name }?.value?.value?.value?.toString()
        }

        fun ActionInteraction.getNumberOptionByName(name: String): Double? {
            val value = this.data.data.options.firstOrNull { it.name == name }?.value?.value?.value
            return when (value) {
                is Double -> value
                is String -> value.toDouble()
                is Int -> value.toDouble()
                else -> null
            }
        }

        fun ActionInteraction.getBooleanOptionByName(name: String): Boolean? {
            return this.data.data.options.firstOrNull { it.name == name }?.value?.value?.value as? Boolean
        }
    }

    suspend fun ActionInteractionCreateEvent.defer(): DeferredMessageInteractionResponseBehavior {
        return when (visibility) {
            Visibility.PUBLIC -> interaction.deferPublicResponse()
            Visibility.PRIVATE -> interaction.deferEphemeralResponse()
        }
    }
}