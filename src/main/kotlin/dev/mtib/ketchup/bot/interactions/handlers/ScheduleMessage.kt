package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.common.DiscordTimestampStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduledInteractionsTable
import dev.mtib.ketchup.bot.interactions.helpers.Interactions.shouldIgnore
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.toMessageFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.ZoneOffset
import java.time.ZonedDateTime
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
        if (event.shouldIgnore()) {
            return
        }
        val response = event.defer()
        try {
            val time = event.interaction.getStringOptionByName("time")!!
            val message = event.interaction.getStringOptionByName("message")!!
            val timeParsed = ZonedDateTime.parse(time, formatter).toInstant()

            val author = event.interaction.user
            val target = event.interaction.channel

            getAnywhere<Database>().transaction {
                ScheduledInteractionsTable.create(
                    target.id.value,
                    author.id.value,
                    message,
                    timeParsed
                )
            }
            "Message scheduled to be sent ${timeParsed.toMessageFormat(DiscordTimestampStyle.RelativeTime)}"
        } catch (e: Exception) {
            e.toString()
        }.let {
            response.respond {
                content = it
            }
        }

    }
}