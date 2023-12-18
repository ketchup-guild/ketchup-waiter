package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.KetchupBot.Companion.MAGIC_WORD

class ArchiveEventsCommand: AdminCommand(
    "archive-events",
    "Archives all events",
    "Archives all events",
) {
    override suspend fun MessageCreateEvent.authorized(kord: Kord) {
        message.reply {
            content = "WIP: Archiving events..."
        }
    }

    override suspend fun Message.matches(kord: Kord): Boolean {
        return content == "$MAGIC_WORD $commandName"
    }
}