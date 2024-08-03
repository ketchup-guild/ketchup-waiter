package dev.mtib.ketchup.bot.interactions.helpers

import dev.kord.core.Kord
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.interactions.handlers.*
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import mu.KotlinLogging

object Interactions {
    private val logger = KotlinLogging.logger { }

    suspend fun register(kord: Kord) {
        interactions.forEach { interactionCommand ->
            logger.info { "Registering interaction ${interactionCommand::class.simpleName}" }
            kord.createGlobalChatInputCommand(interactionCommand.name, interactionCommand.description) {
                this.dmPermission = false
                this.defaultMemberPermissions = null
                interactionCommand.build(this)
            }
            kord.on<ActionInteractionCreateEvent> {
                if (this.interaction.data.data.name.value!! != interactionCommand.name) return@on
                interactionCommand.handleInteraction(this, kord)
            }
        }
    }

    private val interactions = arrayOf<Interaction>(
        Leave,
        Help,
        ReactionSubscribtion,
        ScheduleMessage,
        Rank
    )
}