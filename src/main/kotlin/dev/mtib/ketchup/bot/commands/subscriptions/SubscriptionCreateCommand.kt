package dev.mtib.ketchup.bot.commands.subscriptions

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.commands.AdminCommand
import dev.mtib.ketchup.bot.features.subscriptions.Subscriptions
import dev.mtib.ketchup.bot.features.subscriptions.storage.SubscriptionsTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCommandArgs

class SubscriptionCreateCommand : AdminCommand(
    commandName = "create subscription",
    commandShortDescription = "Create a new subscription",
    commandHelp = "Usage: `create subscription <role>`",
) {
    override val category: Category = Category.Role
    override val completeness: Completeness = Completeness.Complete

    override suspend fun MessageCreateEvent.authorized(kord: Kord) {
        val payload = message.getCommandArgs(this@SubscriptionCreateCommand)
        if (payload.size != 1) {
            message.reply {
                content = "Invalid number of arguments"
            }
            return
        }
        val roleMention = payload[0]
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
        if (existingSubscription != null) {
            message.reply {
                content = "Subscription already exists for this role"
            }
            return
        }

        db.transaction {
            SubscriptionsTable.createSubscription(
                ownerId = message.author!!.id.value,
                roleId = role.id.value,
            )
        }

        message.reply {
            content = """
               Created subscription for role ${role.mention} owned by ${message.author!!.mention}.
               - Use `${magicWord} list subscriptions` to see all subscriptions
               - Use `${magicWord} subscribe <role>` to subscribe to a role
               - Use `${magicWord} unsubscribe <role>` to unsubscribe from a role
               """.trimIndent()
        }
    }
}