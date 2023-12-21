package dev.mtib.ketchup.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.editMemberPermission
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent

class LeaveCommand : ChannelCommand(
    "leave",
    "Lets you leave the channel",
    "Removes yourself from the channel you write this in, if you were individually added (i.e. not through a role).",
) {
    override val category: Category = Category.Role
    override val completeness: Completeness = Completeness.WIP

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        runCatching {
            val channel = message.channel.asChannelOf<TextChannel>()
            val manualPermissions =
                channel.getPermissionOverwritesForMember(author.id)!!
            if (Permission.ViewChannel in manualPermissions.allowed) {
                channel.editMemberPermission(author.id) {
                    allowed -= Permission.ViewChannel
                }
            }
        }.onFailure {
            message.reply { content = "You can't leave this channel because something went wrong: ${it.message}" }
        }
    }
}