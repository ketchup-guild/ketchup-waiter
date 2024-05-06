package dev.mtib.ketchup.bot.commands.subscriptions

import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.commands.ChannelCommand
import dev.mtib.ketchup.bot.commands.subscriptions.IterateMembersCommand.Companion.getAllMembers
import dev.mtib.ketchup.bot.features.subscriptions.Subscriptions
import dev.mtib.ketchup.bot.features.subscriptions.storage.SubscriptionsTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.storage.Storage.Companion.getMagicWord
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCommandBody

class PostSubscriberMessageCommand : ChannelCommand(
    "post",
    "Post a message to all subscribers",
    "Usage: `${getMagicWord()} post <role> <message>` to post a message to all subscribers"
) {
    override val category: Category = Category.Role
    override val completeness: Completeness = Completeness.Complete

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val body = message.getCommandBody(this@PostSubscriberMessageCommand)
        val roleMention = body.substring(0, body.indexOf(" "))
        val role = Subscriptions.getRoleFromMention(roleMention, message)
            .fold(
                ifLeft = {
                    message.reply {
                        content = it
                    }
                    return
                },
                ifRight = { it }
            )

        val db = getAnywhere<Database>()
        val existingSubscription = db.transaction {
            SubscriptionsTable.getSubscriptionByRole(role.id.value)
        }
        if (existingSubscription == null) {
            message.reply {
                content = "This role is not subscribable"
            }
            return
        }

        if (existingSubscription.ownerId != author.id.value) {
            message.reply {
                content = "You are not the owner of this role"
            }
            return
        }

        val messageContent = body.substring(body.indexOf(" ") + 1)

        val fullDmContent = buildString {
            appendLine("**Message from ${message.getAuthorAsMember().mention} to @${role.name}:**")
            appendLine()
            appendLine(messageContent)
            appendLine()
            appendLine("> You are getting this message because you are subscribed to the @${role.name} role.")
            appendLine("> You can unsubscribe by saying `${getMagicWord()} unsubscribe @${role.name}` in a channel I can see.")
            appendLine("> Or tell others to subscribe by saying `${getMagicWord()} subscribe @${role.name}` in a channel I can see.")
            appendLine("> This is a **noreply** message. If you have any questions, please ask in the server.")
        }

        val subscribers = message.getGuild().getAllMembers().filter {
            it.roleIds.contains(role.id)
        }.onEach {
            it.getDmChannel().createMessage {
                content = fullDmContent
            }
        }.toList()

        message.reply {
            content =
                "Message sent to ${subscribers.size} subscribers of ${role.name}:\n\n$messageContent\n\nUsers: ${subscribers.joinToString { it.effectiveName }}"
        }
    }
}