package dev.mtib.ketchup.bot.interactions.helpers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.application.ApplicationCommand
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.interactions.handlers.*
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.utils.isGod
import io.github.oshai.kotlinlogging.KotlinLogging

object Interactions {
    private val logger = KotlinLogging.logger { }
    private const val COMMAND_PREFIX_ENV = "TEMP_COMMAND_PREFIX"
    private const val COMMAND_FILTER_ENV = "TEMP_COMMAND_FILTER"
    private val tempCommandPrefix: String? by lazy { System.getenv(COMMAND_PREFIX_ENV) }
    private val commands = mutableSetOf<ApplicationCommand>()

    suspend fun register(kord: Kord) {
        fun String.tempPrefixed(): String = when {
            tempCommandPrefix != null -> "$tempCommandPrefix-$this"
            else -> this
        }
        filteredInteractions.forEach { interactionCommand ->
            logger.info { "Registering interaction ${interactionCommand::class.simpleName}" }
            val name = interactionCommand.name.tempPrefixed()
            kord.createGlobalChatInputCommand(name, interactionCommand.description) {
                this.dmPermission = false
                this.defaultMemberPermissions = null
                interactionCommand.build(this)
            }.also {
                commands.add(it)
            }
        }
        kord.on<ActionInteractionCreateEvent> {
            filteredInteractions.find { it.name.tempPrefixed() == this.interaction.data.data.name.value!! }
                ?.handleInteraction(this, kord)
        }
        if (tempCommandPrefix != null) {
            val shutdownCommandName = "shutdown".tempPrefixed()
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
        Create,
        Dalle,
        GamesAll,
        GamesBetween,
        GamesFor,
        Ghost,
        Gpt,
        Help,
        Leave,
        News,
        NorthernLights,
        Rank,
        ReactionSubscription,
        RegisterAdventOfCode,
        ReportBenchmark,
        ScheduleEventChannel,
        ScheduleMessage,
        ToggleRespondToGod,
    )

    fun asIterable(): Iterable<Interaction> {
        return interactions.asIterable()
    }

    private val filteredInteractions: List<Interaction> by lazy {
        val regexString = System.getenv(COMMAND_FILTER_ENV)
        if (regexString != null) {
            val re = Regex(regexString)
            return@lazy interactions.filter {
                it.name.matches(re)
            }
        }
        return@lazy interactions.toList()
    }

    fun ActionInteractionCreateEvent.shouldIgnore(): Boolean {
        return this.interaction.user.isBot || (this.interaction.user.isGod && !ToggleRespondToGod.respondToGod)
    }
}