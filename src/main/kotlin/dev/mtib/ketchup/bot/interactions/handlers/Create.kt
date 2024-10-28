package dev.mtib.ketchup.bot.interactions.handlers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.group
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.features.planner.Planner
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.Visibility
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.onSubcommand

object Create : Interaction {
    override val visibility: Visibility = Visibility.PRIVATE
    override val name: String = "create"
    override val description: String = "Create a bunch of different things"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        super.build(it)
        it.group("event", "Create an event") {
            subCommand("private", "Create a private event idea channel") {
                string("name", "Name of the event channel") {
                    required = true
                }
            }
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()

        event.interaction.onSubcommand("event", "private") {
            val name = event.interaction.getStringOptionByName("name")!!
            val cleanedName = "idea-" + name.map {
                when (it) {
                    ' ' -> '-'
                    '_' -> '-'
                    else -> it
                }
            }
                .filter { it.isLetterOrDigit() || it == '-' }.joinToString("")
                .replace("-+".toRegex(), "-") + "-${Planner.BELL}"
            val guildId = event.interaction.data.guildId.value!!
            Planner.createPrivateIdeaChannel(kord, cleanedName, "", event.interaction.user.asMember(guildId))
            response.respond {
                content = "Creating private event channel $cleanedName"
            }
            return
        }

        response.respond {
            content = jacksonObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(event.interaction.data.data.options)
        }
    }
}