package dev.mtib.ketchup.bot.commands.schedule

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.commands.ChannelCommand
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduledMessagesTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ScheduleMessageCommand : ChannelCommand(
    commandName = "schedule",
    commandShortDescription = "Schedule a message to be sent later",
    commandHelp = buildString {
        appendLine("To schedule a message, use the following syntax:")
        appendLine("```")
        appendLine("$magicWord schedule <target> <time> <message>")
        appendLine("```")
        appendLine("Where:")
        appendLine("- `<target>` is the user or channel you want to send the message to")
        appendLine("- `<time>` is the time at which the message should be sent, in the format `YYYY-MM-DD HH:MM:SS` in UTC")
        appendLine("- `<message>` is the message you want to send")
    }
) {
    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val specs = try {
            extractMessageSpecs(message)
        } catch (e: IllegalArgumentException) {
            message.reply {
                content = "Invalid syntax: ${e.message}"
            }
            return
        }
        val db = getAnywhere<Database>()

        try {
            db.transaction {
                ScheduledMessagesTable.create(
                    message = specs.messageIdentification,
                    targetId = specs.target.value,
                    time = specs.time
                )
            }
        } catch (e: Exception) {
            message.reply {
                content = "Failed to schedule message: ${e.message}"
            }
            return
        }

        message.reply {
            content = "Scheduled message to be sent"
        }
    }

    fun extractMessageSpecs(message: String): Specs {
        val match =
            regex.matchEntire(message.removePrefix(prefix).trim()) ?: throw IllegalArgumentException(commandHelp)
        return Specs(
            target = Snowflake(match.groupValues[1]),
            time = ZonedDateTime.parse(match.groupValues[2], formatter).toInstant(),
            content = match.groupValues[3]
        )
    }

    fun extractMessageSpecs(message: Message): IdentifiedSpecs {
        val stringSpecs = extractMessageSpecs(message.content)
        return IdentifiedSpecs(
            target = stringSpecs.target,
            time = stringSpecs.time,
            content = stringSpecs.content,
            messageIdentification = ScheduledMessagesTable.MessageIdentification(
                messageId = message.id.value,
                messageChannelId = message.channelId.value
            )
        )
    }

    open class Specs(
        val target: Snowflake,
        val time: Instant,
        val content: String,
    )

    class IdentifiedSpecs(
        target: Snowflake,
        time: Instant,
        content: String,
        val messageIdentification: ScheduledMessagesTable.MessageIdentification
    ) : Specs(
        target, time, content
    )

    private val regex = Regex("""<[@#](\d+)> `?(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})`? (.+)""")
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
}