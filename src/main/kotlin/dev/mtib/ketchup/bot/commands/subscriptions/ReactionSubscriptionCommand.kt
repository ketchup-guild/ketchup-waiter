package dev.mtib.ketchup.bot.commands.subscriptions

import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.mtib.ketchup.bot.commands.ChannelCommand
import dev.mtib.ketchup.bot.features.subscriptions.reactions.ReactionSubscriptions
import dev.mtib.ketchup.bot.utils.getCommandBody
import dev.mtib.ketchup.bot.utils.messageLink
import kotlinx.datetime.Clock

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
        val author = messageAuthor?.asUser() ?: return
        if (author.isBot) return
        val reactor = user.asUserOrNull() ?: return
        if (reactor.isBot) return
        if (ReactionSubscriptions.isSubscribed(author)) {
            author.getDmChannel().createMessage {
                content =
                    buildString {
                        append("Your message ")
                        append(
                            messageLink(
                                guildId!!,
                                channelId,
                                messageId
                            )
                        )
                        append(" received a reaction ")
                        append(emoji.mention)
                        append(" from ")
                        append(reactor.mention)
                        append(" ")
                        append(Clock.System.now().toMessageFormat(DiscordTimestampStyle.RelativeTime))
                    }
            }
        }
    }

    override suspend fun ReactionRemoveEvent.handleReaction() {
        val author = message.asMessage().author ?: return
        if (author.isBot) return
        val user = user.asUserOrNull() ?: return
        if (user.isBot) return
        if (ReactionSubscriptions.isSubscribed(author)) {
            author.getDmChannel().createMessage {
                content =
                    buildString {
                        append("Your message ")
                        append(
                            messageLink(
                                guildId!!,
                                channelId,
                                messageId
                            )
                        )
                        append(" lost a reaction ")
                        append(emoji.mention)
                        append(" from ")
                        append(user.mention)
                        append(" ")
                        append(Clock.System.now().toMessageFormat(DiscordTimestampStyle.RelativeTime))
                    }
            }
        }
    }
}