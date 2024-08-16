package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.commands.Command
import dev.mtib.ketchup.bot.commands.HelpCommand
import dev.mtib.ketchup.bot.commands.HelpCommand.toLongHelpString
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import mu.KotlinLogging
import org.koin.mp.KoinPlatform

object Help : Interaction {
    private val logger = KotlinLogging.logger { }
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PRIVATE
    override val name: String = "help"
    override val description: String = "Show help"
    private val commands by lazy { KoinPlatform.getKoin().getAll<Command>() }

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.string("command", "The command you want help with") {
            required = false
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()

        val specificCommand = event.interaction.getStringOptionByName("command")
        logger.trace { specificCommand }

        when (specificCommand) {
            null -> HelpCommand.globalHelpMessage
            else -> commands.find { it.name == specificCommand }?.let {
                it.toLongHelpString()
            } ?: "Command not found"
        }.let {
            response.respond {
                content = it
            }
        }

    }
}