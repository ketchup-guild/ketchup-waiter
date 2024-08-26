package dev.mtib.ketchup.bot.features.subscriptions.reactions

import arrow.core.Either
import arrow.core.raise.either
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.common.toMessageFormat
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.mtib.ketchup.bot.commands.subscriptions.ReactionSubscriptionCommand.isIgnoredGod
import dev.mtib.ketchup.bot.features.subscriptions.reactions.storage.ReactionSubscriptionsTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.messageLink
import kotlinx.datetime.Clock

object ReactionSubscriptions {
    val db by lazy { getAnywhere<Database>() }
    fun subscribe(user: User) {
        return db.transaction { ReactionSubscriptionsTable.subscribe(user.id.value) }
    }

    fun unsubscribe(user: User) {
        return db.transaction { ReactionSubscriptionsTable.unsubscribe(user.id.value) }
    }

    fun isSubscribed(user: User): Boolean {
        return db.transaction { ReactionSubscriptionsTable.isSubscribed(user.id.value) }
    }

    sealed class ReactionEvent(
        val author: User,
        val reactor: User,
        val channel: GuildChannel,
        val messageId: Snowflake,
        val emoji: ReactionEmoji,
    ) {
        private val guildId = channel.guildId
        private val channelId = channel.id

        companion object {
            fun shouldIgnore(author: User, reactor: User): Boolean {
                return author.isBot
                        || reactor.isBot
                        || author.isIgnoredGod()
                        || !isSubscribed(author)
            }
        }

        class Add(
            author: User,
            reactor: User,
            channel: GuildChannel,
            messageId: Snowflake,
            emoji: ReactionEmoji,
        ) : ReactionEvent(
            author = author,
            reactor = reactor,
            channel = channel,
            messageId = messageId,
            emoji = emoji,
        ) {
            companion object {
                suspend fun ReactionAddEvent.toSubscriptionEvent(): Either<String, Add> = either {
                    val author = messageAuthor?.asUserOrNull() ?: raise("Author is missing")
                    val reactor = user.asUserOrNull() ?: raise("Reactor is missing")
                    val channel = channel.asChannelOfOrNull<GuildChannel>() ?: raise("Channel is missing")
                    if (shouldIgnore(author, reactor)) raise("Ignoring event")
                    Add(
                        author = author,
                        reactor = reactor,
                        channel = channel,
                        messageId = messageId,
                        emoji = emoji,
                    )
                }
            }
        }

        class Remove(
            author: User,
            reactor: User,
            channel: GuildChannel,
            messageId: Snowflake,
            emoji: ReactionEmoji,
        ) : ReactionEvent(
            author = author,
            reactor = reactor,
            channel = channel,
            messageId = messageId,
            emoji = emoji,
        ) {
            companion object {
                suspend fun ReactionRemoveEvent.toSubscriptionEvent(): Either<String, Remove> = either {
                    val author = message.asMessageOrNull()?.author ?: raise("Author is missing")
                    val reactor = user.asUserOrNull() ?: raise("Reactor is missing")
                    val channel = channel.asChannelOfOrNull<GuildChannel>() ?: raise("Channel is missing")
                    if (shouldIgnore(author, reactor)) raise("Ignoring event")
                    Remove(
                        author = author,
                        reactor = reactor,
                        channel = channel,
                        messageId = messageId,
                        emoji = emoji,
                    )
                }
            }
        }

        suspend fun handle(): Either<String, Unit> = either {
            val status = author.asMemberOrNull(guildId)?.getPresenceOrNull()?.status ?: PresenceStatus.Offline
            val messageLinkStr = messageLink(guildId, channelId, messageId)
            (author.getDmChannelOrNull() ?: raise("Author has no DM channel")).createMessage(
                buildString {
                    when (status) {
                        PresenceStatus.Offline,
                        PresenceStatus.Invisible -> {
                            val reactorName = reactor.asMemberOrNull(guildId)?.effectiveName ?: reactor.effectiveName
                            val emojiMention = emoji.mention
                            val channelName = channel.asChannelOfOrNull<GuildChannel>()?.name?.let { "#$it" }
                                ?: "some channel"
                            when (this@ReactionEvent) {
                                is Add -> {
                                    appendLine("$reactorName reacted $emojiMention in $channelName")
                                }

                                is Remove -> {
                                    appendLine("$reactorName removed $emojiMention reaction in $channelName")
                                }
                            }
                            appendLine()

                            val relativeTime = Clock.System.now().toMessageFormat(DiscordTimestampStyle.RelativeTime)

                            appendLine("> You received the mobile-friendly notification. Here is the rich data: ")
                            appendLine("> Message: $messageLinkStr")
                            appendLine("> Reactor: ${reactor.mention}")
                            appendLine("> Time: $relativeTime")
                        }

                        PresenceStatus.Idle,
                        PresenceStatus.DoNotDisturb,
                        PresenceStatus.Online,
                        is PresenceStatus.Unknown -> {
                            append("Your message ")
                            append(messageLinkStr)
                            when (this@ReactionEvent) {
                                is Add -> {
                                    append(" received a reaction ")
                                }

                                is Remove -> {
                                    append(" had a reaction ")
                                }
                            }
                            append(emoji.mention)
                            when (this@ReactionEvent) {
                                is Add -> {
                                    append(" by ")
                                }

                                is Remove -> {
                                    append(" removed by ")
                                }
                            }
                            append(reactor.mention)
                            append(" ")
                            append(Clock.System.now().toMessageFormat(DiscordTimestampStyle.RelativeTime))
                        }
                    }
                }
            )
        }
    }
}