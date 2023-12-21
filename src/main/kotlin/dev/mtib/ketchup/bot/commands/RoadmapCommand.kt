package dev.mtib.ketchup.bot.commands

import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent

class RoadmapCommand : ChannelCommand(
    "roadmap",
    "Roadmap",
    "Roadmap",
) {
    override val completeness: Completeness = Completeness.WIP
    override val category: Category = Category.Misc

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        message.reply {
            content = buildString {
                appendLine("**Roadmap:** (i.e. features that are planned to be added)")
                appendLine()
                listOf(
                    "Channel: leave a channel you were manually added to",
                    "Channel: close channel (i.e. archive and remove all members) on inactivity",
                    "Event channel: archive past events",
                    "Event channel: notify on upcoming events (e.g. 1 day before)",
                    "Event channel: list upcoming events and allow to join",
                    "Games: Blackjack",
                    "Games: X of these people are lying",
                    "Rallly integration: notify on completed rallly in event or club channels",
                    "Rallly integration: notify on stale rallly in event or club channels",
                    "Role: change roles (location, employment, etc.)",
                    "AI: bad text generator based on people's messages",
                    "AI: DALL·E image generator based on people's messages",
                    "Convenience: improve Direct Message experience"
                ).map {
                    val (category, description) = it.split(": ")
                    category to description
                }.groupBy { it.first }.forEach { (category, items) ->
                    appendLine("- **$category**")
                    items.forEach { (_, description) ->
                        appendLine("  - $description")
                    }
                }
                appendLine()
                appendLine("You're welcome to help out and contribute to the bot!")
                appendLine("The code is open on GitHub: https://github.com/ketchup-guild/ketchup-waiter")
            }
        }
    }
}