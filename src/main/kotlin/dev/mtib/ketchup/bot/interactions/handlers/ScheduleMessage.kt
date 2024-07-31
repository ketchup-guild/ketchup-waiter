package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getOptionValueByName
import mu.KotlinLogging
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object ScheduleMessage : Interaction {
    private val logger = KotlinLogging.logger { }
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PRIVATE
    override val name: String = "schedule_message"
    override val description: String = "schedule a message to be sent at a specific time"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.string("time", "time to send the message in the format yyyy-MM-dd HH:mm:ss in UTC") {
            required = true
        }
        it.string("message", "message to send") {
            required = true
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()
        try {
            val time = event.interaction.getOptionValueByName("time")
            val message = event.interaction.getOptionValueByName("message")
            val timeParsed = formatter.parse(time)

            val author = event.interaction.user
            val target = event.interaction.channel
        } catch (e: Exception) {
            logger.warn { e.toString() }
        }

        response.respond {
            content =
                "Not yet implemented, because using application commands the body will have to be stored in the db (unlike with the chat command which can just store the id of the message to send later)"
        }
    }
}