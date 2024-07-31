package dev.mtib.ketchup.bot.commands.organisation

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
import dev.mtib.ketchup.bot.commands.ChannelCommand
import dev.mtib.ketchup.bot.commands.HelpCommand.toLongHelpString
import dev.mtib.ketchup.bot.storage.Storage.Emoji
import dev.mtib.ketchup.bot.storage.Storage.MagicWord
import dev.mtib.ketchup.bot.utils.createPrivateChannelFor
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCategoryByNameOrNull
import dev.mtib.ketchup.bot.utils.getCommandArgs
import mu.KotlinLogging

class CreateEventCommand(private val magicWordBoot: MagicWord) : ChannelCommand(
    COMMAND,
    "Creates an event channel",
    buildString {
        appendLine("Creates an event channel that people can join.\n")
        appendLine("**Usage**:")
        appendLine("- `$magicWordBoot $COMMAND <yyyy>-<mm>-<dd> <topic-slug> <description>`")
        appendLine("- `$magicWordBoot $COMMAND tbd <topic-slug> <description>`")
        appendLine("\n**Example**:")
        appendLine("- `$magicWordBoot $COMMAND 2025-12-24 xmas-party Let's hang out!`")
        appendLine("- `$magicWordBoot $COMMAND tbd cooking-hangout Let's hang out and cook something some time!`")
    },
) {
    private val logger = KotlinLogging.logger { }
    override val category = Category.Event
    override val completeness = Completeness.Complete

    companion object {
        val CALLS_TO_ARMS_REGEX = Regex("""\*\*Channel:\*\* <#(\d+)>""")
        val DATE_REGEX = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})|tbd""")
        const val COMMAND = "event create"
    }

    private suspend fun Message.callsToArms(): Channel {
        val match = CALLS_TO_ARMS_REGEX.find(content) ?: error("No calls to arms found")
        val channelId = match.groupValues[1].toLong()
        return kord.getChannel(Snowflake(channelId)) ?: error("Channel $channelId not found")
    }

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val args = message.getCommandArgs(this@CreateEventCommand)
        if (args.size < 3) {
            message.reply {
                content = this@CreateEventCommand.toLongHelpString()
            }
            return
        }
        try {
            if (message.content.startsWith("$magicWord $name ")) {
                val requestData = message.content.removePrefix("$magicWord $name ").trim()
                val date = args[0]
                val topicSlug = args[1]
                val description = args.drop(2).joinToString(" ")

                val match = DATE_REGEX.find(date)
                if (match == null) {
                    message.reply {
                        content = "Error: date must be in format `yyyy-mm-dd` or `tbd`"
                    }
                    return
                }

                val dateReformat = if (date == "tbd") {
                    "tbd"
                } else {
                    val (_, year, month, day) = match.groupValues
                    "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
                }.lowercase()

                val guild = message.getGuild()
                val category = guild.getCategoryByNameOrNull("Upcoming Events")
                if (category == null) {
                    message.reply {
                        content = "Error: upcoming event category not found"
                    }
                    return
                }
                val createdChannel = guild.createPrivateChannelFor(
                    "${dateReformat}-${topicSlug.lowercase()}",
                    description,
                    author,
                    category.id,
                )

                if (date == "tbd") {
                    createdChannel.createMessage(buildString {
                        appendLine("This channel was created by ${author.mention} for an event that doesn't have a date yet.")
                        appendLine()
                        appendLine("**Description**: $description")
                        appendLine()
                        appendLine("I recommend you use https://rallly.co/ to schedule a date for this event.")
                        append("If you post a rallly link in this channel I'll pin it for you. ")
                        appendLine("There are even some commands to help you with that, see `$magicWord help` for options (WIP).")
                    })
                } else {
                    createdChannel.createMessage(buildString {
                        appendLine("This channel was created by ${author.mention} for an event on $dateReformat.")
                        appendLine()
                        appendLine("**Description**: $description")
                        appendLine()
                        appendLine("There are even some commands to help you with managing the channel, see `$magicWord help` for options (WIP).")
                    })
                }

                val joinEmoji = getAnywhere<Emoji>().join
                val callToArmsMessage = message.channel.createMessage(buildString {
                    appendLine("Hey, @here! There's a new event!")
                    appendLine()
                    appendLine("**Date:** $dateReformat")
                    appendLine("**Channel:** ${createdChannel.mention}")
                    appendLine("**Admin:** ${author.mention}")
                    appendLine(
                        "**Description:**\n${
                            description.lines().joinToString("\n") { "> $it" }
                        }\n\nReact to this message with the $joinEmoji emoji to join the event channel for more information!"
                    )
                })
                callToArmsMessage.addReaction(ReactionEmoji.Unicode(joinEmoji))
            }
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