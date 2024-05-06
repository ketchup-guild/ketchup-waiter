package dev.mtib.ketchup.bot.commands.subscriptions

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.commands.ChannelCommand
import dev.mtib.ketchup.bot.features.subscriptions.storage.SubscriptionsTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

class ListSubscriptionsCommand : ChannelCommand(
    "list subscriptions",
    "Lists all subscriptions",
    "Lists all subscriptions"
) {
    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val db = getAnywhere<Database>()
        val subscriptions = db.transaction {
            SubscriptionsTable.getAllSubscriptions()
        }
        if (subscriptions.isEmpty()) {
            message.reply {
                content = "No subscriptions found"
            }
            return
        }
        message.reply {
            content = "You can subscribe to the following roles:\n" + subscriptions.asFlow().map {
                val role = message.getGuild().getRole(Snowflake(it.roleId))
                val user = kord.getUser(Snowflake(it.ownerId))?.asMember(message.getGuild().id)
                "- ${role.name} (created by ${user?.effectiveName ?: "unknown"})"
            }.toList().joinToString("\n") + "\n\nUse `$magicWord subscribe <role>` to subscribe to a role"
        }
    }
}