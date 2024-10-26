package dev.mtib.ketchup.bot.commands.subscriptions

import arrow.core.flatMap
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.mtib.ketchup.bot.commands.ChannelCommand
import dev.mtib.ketchup.bot.features.subscriptions.reactions.ReactionSubscriptions
import dev.mtib.ketchup.bot.features.subscriptions.reactions.ReactionSubscriptions.ReactionEvent.Add.Companion.toSubscriptionEvent
import dev.mtib.ketchup.bot.features.subscriptions.reactions.ReactionSubscriptions.ReactionEvent.Remove.Companion.toSubscriptionEvent
import dev.mtib.ketchup.bot.utils.getCommandBody
import io.github.oshai.kotlinlogging.KotlinLogging

object ReactionSubscriptionCommand : ChannelCommand(
    "reaction",
    "Subscribe to reaction updates",
    """
        Subscribe to reactions on all your messages the bot can see.
        
        Usage:
        
        - `$magicWord reaction subscribe`: Subscribe to reactions
        - `$magicWord reaction unsubscribe`: Unsubscribe from reactions 
         
        While subscribed any reaction you get on a message of yours that the bot can see will be sent to you in a DM.
    """.trimIndent(),
) {
    override val category: Category = Category.Misc
    override val completeness: Completeness = Completeness.Complete
    val logger = KotlinLogging.logger { }

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val body = message.getCommandBody(this@ReactionSubscriptionCommand)

        when (body) {
            "subscribe" -> {
                ReactionSubscriptions.subscribe(author)
                message.reply {
                    content = "You are now subscribed to reactions"
                }
            }

            "unsubscribe" -> {
                ReactionSubscriptions.unsubscribe(author)
                message.reply {
                    content = "You are now unsubscribed from reactions"
                }
            }

            else -> {
                message.reply {
                    content = "Unknown command"
                }
            }
        }
    }

    override suspend fun ReactionAddEvent.handleReaction() {
        this.toSubscriptionEvent().flatMap { it.handle() }.onLeft {
            logger.debug { it }
        }
    }

    override suspend fun ReactionRemoveEvent.handleReaction() {
        this.toSubscriptionEvent().flatMap { it.handle() }.onLeft {
            logger.debug { it }
        }
    }
}