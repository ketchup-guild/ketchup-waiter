package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.storage.Storage.MagicWord
import dev.mtib.ketchup.bot.utils.getAnywhere

class ArchiveEventsCommand : AdminCommand(
    "archive",
    "Archives past channels",
    "Archives channels with dates in the past",
) {
    override val category = Category.Admin
    override val completeness = Completeness.Stubbed
    override suspend fun MessageCreateEvent.authorized(kord: Kord) {
        message.reply {
            content = "WIP: Archiving events..."
        }
    }
}