package dev.mtib.ketchup.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.editMemberPermission
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.mtib.ketchup.bot.commands.HelpCommand.Companion.toLongHelpString
import dev.mtib.ketchup.bot.storage.Storage.Emoji
import dev.mtib.ketchup.bot.storage.Storage.MagicWord
import dev.mtib.ketchup.bot.utils.createPrivateChannelFor
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCategoryByNameOrNull
import dev.mtib.ketchup.bot.utils.getCommandArgs
import mu.KotlinLogging

class CreateClubCommand(private val magicWord: MagicWord) : ChannelCommand(
    COMMAND,
    "Creates a club channel",
    buildString {
        appendLine("Creates a club channel that people can join.\n")
        appendLine("**Usage**:")
        appendLine("- `$magicWord $COMMAND <topic-slug> <description>`")
        appendLine("\n**Example**:")
        appendLine("- `$magicWord $COMMAND food Let's talk about food!`")
        appendLine("- `$magicWord $COMMAND anime Let's talk about weeb stuff!`")
    },
) {
    private val logger = KotlinLogging.logger { }
    override val category = Category.Club
    override val completeness = Completeness.Complete

    companion object {
        val CALLS_TO_ARMS_REGEX = Regex("""\*\*Channel:\*\* <#(\d+)>""")
        const val COMMAND = "club create"
    }

    private suspend fun Message.callsToArms(): Channel {
        val match = CALLS_TO_ARMS_REGEX.find(content) ?: error("No calls to arms found")
        val channelId = match.groupValues[1].toLong()
        return kord.getChannel(Snowflake(channelId)) ?: error("Channel $channelId not found")
    }

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val args = message.getCommandArgs(this@CreateClubCommand)
        if (args.isEmpty()) {
            message.reply {
                content = this@CreateClubCommand.toLongHelpString()
            }
            return
        }
        try {
            val topicSlug = args[0]
            val description = args.drop(1).joinToString(" ")

            val guild = message.getGuild()
            val category = guild.getCategoryByNameOrNull("Clubs")
            if (category == null) {
                message.reply {
                    content = "Error: club category not found"
                }
                return
            }
            val createdChannel = guild.createPrivateChannelFor(
                topicSlug,
                description,
                author,
                category.id,
            )

            createdChannel.createMessage(buildString {
                appendLine("This channel was created by ${author.mention} for a club.")
                appendLine()
                appendLine("**Description**: $description")
                appendLine()
                appendLine("There are even some commands to help you with managing this channel, see `$magicWord help` for options (WIP).")
            })

            val joinEmoji = getAnywhere<Emoji>().join
            val callToArmsMessage = message.channel.createMessage(buildString {
                appendLine("Hey, @here! There's a new club!")
                appendLine()
                appendLine("**Channel:** ${createdChannel.mention}")
                appendLine("**Creator:** ${author.mention}")
                appendLine(
                    "**Description:**\n${
                        description.lines().joinToString("\n") { "> $it" }
                    }\n\nReact to this message with the $joinEmoji emoji to join the event channel for more information!"
                )
            })
            callToArmsMessage.addReaction(ReactionEmoji.Unicode(joinEmoji))
        } catch (e: Exception) {
            message.reply {
                content = "Error: ${e.message}"
            }
        }
    }

    override suspend fun ReactionAddEvent.handleReaction() {
        runCatching {
            val joinEmoji = getAnywhere<Emoji>().join
            if (emoji.name == joinEmoji && message.asMessage().author?.isBot == true) {
                val callToArmsChannel = message.asMessage().callsToArms()
                val user = user.asUser()
                val currentPermissions = callToArmsChannel.asChannelOf<TextChannel>()
                    .getEffectivePermissions(user.id)
                if (Permission.ViewChannel in currentPermissions) {
                    // User is already in the channel.
                    return
                }
                callToArmsChannel
                    .asChannelOf<TextChannel>()
                    .editMemberPermission(user.id) {
                        reason = "Automation: user reacted to channel join call to arms"
                        allowed = Permissions(
                            Permission.ViewChannel,
                            Permission.SendMessages,
                            Permission.ReadMessageHistory,
                            Permission.AddReactions,
                        )
                    }
                logger.debug { "User ${user.mention} reacted with $joinEmoji in ${message.channel.mention} with intent to join ${callToArmsChannel.mention}" }
            }
        }
    }

    override suspend fun ReactionRemoveEvent.handleReaction() {
        runCatching {
            val joinEmoji = getAnywhere<Emoji>().join
            if (emoji.name == joinEmoji && message.asMessage().author?.isBot == true) {
                val callToArmsChannel = message.asMessage().callsToArms()
                val user = user.asUser()
                val isChannelAdmin = Permission.ManageChannels in callToArmsChannel.asChannelOf<TextChannel>()
                    .getEffectivePermissions(user.id)
                if (isChannelAdmin) {
                    // Admin can't leave this way.
                    return
                }
                callToArmsChannel
                    .asChannelOf<TextChannel>()
                    .editMemberPermission(user.id) {
                        reason = "Automation: user removed reaction to channel join call to arms"
                        if (Permission.ManageChannels !in allowed) {
                            allowed = Permissions()
                        }
                    }
                logger.debug { "User ${user.mention} removed reaction of $joinEmoji in ${message.channel.mention} with intent to leave ${callToArmsChannel.mention}" }
            }
        }
    }
}