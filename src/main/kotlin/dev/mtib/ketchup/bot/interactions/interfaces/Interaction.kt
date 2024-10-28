package dev.mtib.ketchup.bot.interactions.interfaces

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

        private inline fun <reified T> ActionInteraction.getOptionByNameOrNull(name: String): T? {
            val entry = data.data.options.value ?: return null

            entry.forEach { current ->
                if (current.name == name) {
                    val v = current.value.value?.value
                    if (v != null && v is T) {
                        return v
                    }
                }

                current.values.value?.forEach { arg ->
                    if (arg.name == name) {
                        val v = arg.value
                        if (v != null && v is T) {
                            return v
                        }
                    }
                }
            }

            return null
        }

        fun ActionInteraction.isSubcommand(name: String): Boolean {
            return data.data.options.value?.any { it.name == name } ?: false
        }

        suspend fun ActionInteraction.onSubcommand(name: String, block: suspend ActionInteraction.() -> Unit) {
            if (isSubcommand(name)) {
                block()
            }
        }

        fun ActionInteraction.getStringOptionByName(name: String): String? {
            return getOptionByNameOrNull(name)
        }

        fun ActionInteraction.getNumberOptionByName(name: String): Double? {
            return when (val value = getOptionByNameOrNull<Any>(name)) {
                is Double -> value
                is String -> value.toDouble()
                is Int -> value.toDouble()
                else -> null
            }
        }

        fun ActionInteraction.getBooleanOptionByName(name: String): Boolean? {
            return getOptionByNameOrNull(name)
        }
    }

    suspend fun ActionInteractionCreateEvent.defer(): DeferredMessageInteractionResponseBehavior {
        return when (visibility) {
            Visibility.PUBLIC -> interaction.deferPublicResponse()
            Visibility.PRIVATE -> interaction.deferEphemeralResponse()
        }
    }
}