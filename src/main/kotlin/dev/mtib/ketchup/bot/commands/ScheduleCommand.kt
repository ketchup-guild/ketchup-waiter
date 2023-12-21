package dev.mtib.ketchup.bot.commands

import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.commands.HelpCommand.Companion.toLongHelpString
import dev.mtib.ketchup.bot.storage.Storage.MagicWord
import dev.mtib.ketchup.bot.utils.KetchupDate.Companion.toKetchupDate
import dev.mtib.ketchup.bot.utils.getAnywhere

class ScheduleCommand : ChannelCommand(
    "event schedule",
    "Update the date of an event",
    "Update the date of an event like `${getAnywhere<MagicWord>()} event schedule 2025-12-31`.",
) {
    override val category: Category = Category.Event
    override val completeness: Completeness = Completeness.WIP

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        runCatching {
            val subCommand = message.content.removePrefix("${getAnywhere<MagicWord>()} event schedule ").trim()
            val dateString = if (subCommand == "tbd") "tbd" else subCommand.toKetchupDate().toString()
            val textChannel = message.channel.asChannelOf<TextChannel>()
            val channelName = textChannel.name
            val parentCategory = textChannel.category!!.asChannel().name

            if (parentCategory.lowercase() != "Upcoming Events".lowercase()) {
                message.reply { content = "This command can only be used in the Upcoming Events category." }
                return
            }

            val suffix = """(\d{4}-\d{1,2}-\d{1,2}|tbd)-(.+)""".toRegex().matchEntire(channelName)!!.groupValues[2]
            if (suffix.isBlank()) {
                message.reply { content = "This command can only be used in event channels." }
                return
            }
            textChannel.edit {
                name = "${dateString}-${suffix}"
            }
            message.reply {
                content = "Event date updated to ${dateString}."
            }
        }.onFailure {
            message.reply {
                content = "Something went wrong: ${it.message}\n\n${this@ScheduleCommand.toLongHelpString()}"
            }
        }
    }
}