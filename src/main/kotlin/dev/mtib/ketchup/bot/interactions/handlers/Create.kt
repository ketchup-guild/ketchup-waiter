package dev.mtib.ketchup.bot.interactions.handlers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.group
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
import dev.mtib.ketchup.bot.features.planner.Planner
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.Visibility
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.onSubcommand
import dev.mtib.ketchup.bot.utils.ketchupZone
import dev.mtib.ketchup.server.auth.AuthClient
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.time.toJavaDuration
import java.time.Instant as JavaTimeInstant

object Create : Interaction {
    override val visibility: Visibility = Visibility.PRIVATE
    override val name: String = "create"
    override val description: String = "Create a bunch of different things"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        super.build(it)
        // Group related subcommands together
        it.group("event", "Create an event") {
            // Define subcommand with clear description
            subCommand("private", "Create a private event idea channel") {
                // Use descriptive parameter name and description
                string("name", "Name of the event channel") {
                    required = true
                }
            }
        }

        // Define standalone subcommand
        it.subCommand("token", "Create a token")
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

        event.interaction.onSubcommand("token") {
            response.respond {
                content = "Creating token, you will create a direct message with the token."
            }
            kord.launch {
                val snowflake = event.interaction.user.id.toString()
                val token = AuthClient.createToken(snowflake)

                user.getDmChannel().createMessage {
                    content = """
                        Hi there, it looks like you just requested an access token to use the ketchup bot API.
                        Your access token is `$token`. This token is valid until ${
                        JavaTimeInstant.now().plus(AuthClient.validity.toJavaDuration()).atZone(
                            ketchupZone
                        ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
                    }.
                        Your account's snowflake id is `${snowflake}`.

                        Please keep this token safe and do not share it with anyone. Generating this token also invalidated any previous tokens you may have had.

                        If this wasn't you, please ignore this message.
                    """.trimIndent()
                }
            }
            return
        }

        response.respond {
            content = jacksonObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(event.interaction.data.data.options)
        }
    }
}
