package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.interaction.user
import dev.mtib.ketchup.bot.interactions.helpers.Interactions.shouldIgnore
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getUserOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.onSubcommand
import dev.mtib.ketchup.bot.features.hideandseek.HideAndSeek as HideAndSeekFeature

object HideAndSeek : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PUBLIC
    override val name: String = "hns"
    override val description: String = "Hide and Seek game commands"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        // Define subcommands with clear descriptions
        it.subCommand("start", "Start a new Hide and Seek game")

        it.subCommand("player", "Add a player to a team") {
            // Use descriptive parameter names and descriptions
            user("mention", "The player to add") {
                required = true
            }
            string("team", "The team to add the player to") {
                required = true
            }
        }

        it.subCommand("shuffle", "Shuffle teams and assign hiding/seeking roles")

        it.subCommand("hide", "Start the hiding phase")

        it.subCommand("found", "Mark a team as having found the hiding team") {
            string("team", "The team that found the hiding team") {
                required = true
            }
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        if (event.shouldIgnore()) {
            return
        }

        val response = event.defer()
        val hideAndSeek = HideAndSeekFeature

        // Get the guild from the interaction
        val guild = event.interaction.data.guildId.value?.let { kord.getGuild(it) }
        if (guild == null) {
            response.respond {
                content = "This command can only be used in a server."
            }
            return
        }

        // Get the channel from the interaction
        val channel = event.interaction.channel.asChannel() as TextChannel

        // Create a temporary message to use with the HideAndSeek feature
        val tempMessage = channel.createMessage("Processing Hide and Seek command...")

        try {
            event.interaction.onSubcommand("start") {
                hideAndSeek.startGame(tempMessage)
                response.respond {
                    content = "Started a new Hide and Seek game!"
                }
                return@handleInteraction
            }

            event.interaction.onSubcommand("player") {
                val user = event.interaction.getUserOptionByName("mention")
                val team = event.interaction.getStringOptionByName("team")

                if (user == null) {
                    response.respond {
                        content = "Error: User mention is missing or invalid. Please use the format `/hns player mention: @User team: teamname`."
                    }
                    return@handleInteraction
                }

                if (team == null) {
                    response.respond {
                        content = "Error: Team name is missing. Please use the format `/hns player mention: @User team: teamname`."
                    }
                    return@handleInteraction
                }

                hideAndSeek.addPlayer(tempMessage, user.mention, team)
                response.respond {
                    content = "Added ${user.username} to team $team."
                }
                return@handleInteraction
            }

            event.interaction.onSubcommand("shuffle") {
                hideAndSeek.shuffleTeams(tempMessage)
                response.respond {
                    content = "Teams have been shuffled and roles assigned."
                }
                return@handleInteraction
            }

            event.interaction.onSubcommand("hide") {
                hideAndSeek.startHiding(tempMessage)
                response.respond {
                    content = "The hiding phase has started!"
                }
                return@handleInteraction
            }

            event.interaction.onSubcommand("found") {
                val team = event.interaction.getStringOptionByName("team")!!

                hideAndSeek.teamFound(tempMessage, team)
                response.respond {
                    content = "Team $team has found the hiding team!"
                }
                return@handleInteraction
            }
        } finally {
            // Delete the temporary message
            tempMessage.delete()
        }

        // If we get here, no subcommand matched
        response.respond {
            content =
                "Unknown subcommand. Available commands: `/hns start`, `/hns player`, `/hns shuffle`, `/hns hide`, `/hns found`"
        }
    }
}
