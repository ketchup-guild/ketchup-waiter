package dev.mtib.ketchup.bot.commands.schedule

import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.commands.AdminCommand
import dev.mtib.ketchup.bot.features.scheduler.Scheduler
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduleTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.toMessageFormat
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce

object ScheduledMessageStatsCommand : AdminCommand(
    commandName = "stats scheduler",
    commandShortDescription = "Get information about scheduled messages",
    commandHelp = "Get information about scheduled messages"
) {
    override suspend fun MessageCreateEvent.authorized(kord: Kord) {
        val db = getAnywhere<Database>()

        val scheduledMessages = db.transaction {
            ScheduleTable.getUnsentMessages().toList()
        }

        suspend fun Snowflake.mention() = Scheduler.Target.fromULong(kord, this.value).mention

        val summary = scheduledMessages.asFlow().map { task ->
            buildString {
                val scheduledMessage = task.messageIdentification.getFromSupplier(kord.defaultSupplier)
                val spec = ScheduleMessageCommand.extractMessageSpecs(scheduledMessage)
                appendLine("> Content length: ${spec.content.length}")
                appendLine(
                    "> Target: ${
                        spec.target.mention()
                    }"
                )
                appendLine(
                    "> Sending ${spec.time.toMessageFormat(DiscordTimestampStyle.RelativeTime)}"
                )
                appendLine(
                    "> Created ${task.createdAt.toMessageFormat(DiscordTimestampStyle.RelativeTime)}"
                )
                appendLine(
                    "" +
                            "> Author: ${
                                scheduledMessage.author!!.asUser().mention
                            }"
                )
            }
        }.reduce { acc, s -> acc + "\n" + s }
        message.reply {
            content = buildString {
                appendLine("## Scheduled messages")
                appendLine(summary)
            }
        }
    }
}