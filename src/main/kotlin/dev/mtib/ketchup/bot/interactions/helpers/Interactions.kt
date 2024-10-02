package dev.mtib.ketchup.bot.interactions.helpers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.application.ApplicationCommand
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.interactions.handlers.*
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.utils.isGod
import mu.KotlinLogging

object Interactions {
    private val logger = KotlinLogging.logger { }
    private const val COMMAND_PREFIX_ENV = "TEMP_COMMAND_PREFIX"
    private val tempCommandPrefix: String? by lazy { System.getenv(COMMAND_PREFIX_ENV) }
    private val commands = mutableSetOf<ApplicationCommand>()

    suspend fun register(kord: Kord) {
        interactions.forEach { interactionCommand ->
            logger.info { "Registering interaction ${interactionCommand::class.simpleName}" }
            val name = if (tempCommandPrefix != null) {
                "$tempCommandPrefix-${interactionCommand.name}"
            } else {
                interactionCommand.name
            }
            kord.createGlobalChatInputCommand(name, interactionCommand.description) {
                this.dmPermission = false
                this.defaultMemberPermissions = null
                interactionCommand.build(this)
            }.also {
                commands.add(it)
            }
            kord.on<ActionInteractionCreateEvent> {
                if (this.interaction.data.data.name.value!! != name) return@on
                interactionCommand.handleInteraction(this, kord)
            }
        }
        if (tempCommandPrefix != null) {
            val shutdownCommandName = "$tempCommandPrefix-shutdown"
            kord.createGlobalChatInputCommand(shutdownCommandName, "Clean shutdown") {
                this.dmPermission = true
                this.defaultMemberPermissions = null
            }.also {
                commands.add(it)
            }
            kord.on<ActionInteractionCreateEvent> {
                if (this.interaction.data.data.name.value!! != shutdownCommandName) return@on
                this.interaction.deferEphemeralResponse().respond {
                    content = "Shutting down"
                }
                kord.shutdown()
            }
        }
    }

    suspend fun unregister() {
        if (tempCommandPrefix != null) {
            commands.forEach {
                it.delete()
            }
        }
    }

    private val interactions = arrayOf<Interaction>(
        Leave,
        Help,
        ReactionSubscription,
        ScheduleMessage,
        Rank,
        GamesFor,
        GamesAll,
        GamesBetween,
        ToggleRespondToGod,
        Gpt,
        Dalle,
        NorthernLights,
        Ghost,
    )

    fun asIterable(): Iterable<Interaction> {
        return interactions.asIterable()
    }

    fun ActionInteractionCreateEvent.shouldIgnore(): Boolean {
        return this.interaction.user.isBot || (this.interaction.user.isGod && !ToggleRespondToGod.respondToGod)
    }
}