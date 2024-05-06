package dev.mtib.ketchup.bot.commands.subscriptions

import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.commands.ChannelCommand
import dev.mtib.ketchup.bot.features.subscriptions.Subscriptions
import dev.mtib.ketchup.bot.features.subscriptions.storage.SubscriptionsTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.storage.Storage.Companion.getMagicWord
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCommandArgs

class SubscribeCommand : ChannelCommand(
    "subscribe",
    "Subscribe to a role",
    "Usage: `${getMagicWord()} subscribe <role>` to subscribe to a role"
) {
    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val args = message.getCommandArgs(this@SubscribeCommand)

        if (args.size != 1) {
            message.reply {
                content = "Invalid number of arguments"
            }
            return
        }

        val roleMention = args[0]
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

        if (message.getAuthorAsMember().roleIds.contains(role.id)) {
            message.reply {
                content = "You are already subscribed to this role"
            }
            return
        }

        message.getAuthorAsMember().addRole(role.id, "Subscribed to role ${role.name}")
        message.reply { content = "Subscribed to role ${role.name}" }
    }
}