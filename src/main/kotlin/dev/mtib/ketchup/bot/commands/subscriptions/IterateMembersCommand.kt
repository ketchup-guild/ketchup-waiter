package dev.mtib.ketchup.bot.commands.subscriptions

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.behavior.requestMembers
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.gateway.PrivilegedIntent
import dev.mtib.ketchup.bot.commands.AdminCommand
import kotlinx.coroutines.flow.map

class IterateMembersCommand : AdminCommand(
    "iterate members",
    "Iterate over all members of the server",
    "Usage: `iterate members`"
) {
    companion object {
        @OptIn(PrivilegedIntent::class)
        suspend fun Guild.getAllMembers(): List<Member> {
            val members = mutableSetOf<Member>()
            requestMembers().map {
                it.members
            }.collect {
                members.addAll(it)
            }
            return members.toList()
        }
    }

    override suspend fun MessageCreateEvent.authorized(kord: Kord) {
        val members = message.getGuild().getAllMembers()
        message.reply {
            content = "Members: ${members.size}\n\n${members.joinToString("\n") { "- ${it.effectiveName}" }}"
        }
    }
}