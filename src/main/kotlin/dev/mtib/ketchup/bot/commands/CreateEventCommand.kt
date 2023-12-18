package dev.mtib.ketchup.bot.commands

import dev.kord.common.entity.*
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.editMemberPermission
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.Category
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.live.channel.live
import dev.kord.core.live.live
import dev.kord.core.on
import dev.mtib.ketchup.bot.KetchupBot.Companion.GOD_IDS
import dev.mtib.ketchup.bot.KetchupBot.Companion.MAGIC_WORD
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import mu.KotlinLogging
import kotlin.time.Duration.Companion.seconds

class CreateEventCommand: Command(
    "create-event",
    "Creates an event",
    buildString {
        appendLine("Creates an event that people can join.")
        appendLine("Usage: `$MAGIC_WORD create-event <yyyy>-<mm>-<dd> <topic-slug> <description>`")
        appendLine("Example: `$MAGIC_WORD create-event 2021-09-25 my-event This is my event`")
    },
) {
    val logger = KotlinLogging.logger { }
    companion object {
        const val JOIN_EMOJI = "👀"
        val CALLS_TO_ARMS_REGEX = Regex("""\*\*Channel:\*\* <#(\d+)>""")
    }

    private suspend fun Message.callsToArms(): Channel {
        val match = CALLS_TO_ARMS_REGEX.find(content) ?: error("No calls to arms found")
        val channelId = match.groupValues[1].toLong()
        return kord.getChannel(Snowflake(channelId)) ?: error("Channel $channelId not found")
    }

    override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            val author = message.author
            if (author?.isBot != false) {
                return@on
            }
            if (message.content == "$MAGIC_WORD $commandName") {
                message.reply {
                    content = commandHelp
                }
            }
            try {
                if (message.content.startsWith("$MAGIC_WORD $commandName ")) {
                    val requestData = message.content.removePrefix("$MAGIC_WORD $commandName ").trim()
                    val (date, topicSlug, description) = requestData.split(" ", limit = 3)

                    message.delete("Automation")

                    val guild = message.getGuild()
                    val category = run {
                        guild.channelBehaviors.find {
                            val category = it.asChannelOfOrNull<Category>()
                            if (category != null) {
                                category.name.lowercase() == "upcoming events"
                            } else {
                                false
                            }
                        }
                    }
                    if (category == null) {
                        message.reply {
                            content = "Error: upcoming event category not found"
                        }
                        return@on
                    }
                    val createdChannel: Channel = guild.createTextChannel(
                        "${date}-${topicSlug}",
                    ) {
                        topic = description
                        reason = "Automation"
                        parentId = category.id
                        permissionOverwrites = buildSet {
                            add(
                                Overwrite(
                                    id = guild.everyoneRole.id,
                                    type = OverwriteType.Role,
                                    deny = Permissions(Permission.ViewChannel),
                                    allow = Permissions(),
                                )
                            )
                            add(
                                Overwrite(
                                    id = author.id,
                                    type = OverwriteType.Member,
                                    allow = Permissions(
                                        Permission.ViewChannel,
                                        Permission.ManageMessages,
                                        Permission.ManageChannels,
                                        Permission.ManageRoles,
                                    ),
                                    deny = Permissions(),
                                )
                            )
                        }.toMutableSet()
                    }

                    val callToArmsMessage = message.channel.createMessage(buildString {
                        appendLine("Hey, @here! There's a new event!")
                        appendLine()
                        appendLine("**Date:** $date")
                        appendLine("**Channel:** ${createdChannel.mention}")
                        appendLine("**Admin:** ${author.mention}")
                        appendLine("**Description:**\n${description.lines().joinToString("\n") { "> $it" }}\n\nReact to this message with the $JOIN_EMOJI emoji to join the event channel for more information!")
                    })
                    callToArmsMessage.addReaction(ReactionEmoji.Unicode(JOIN_EMOJI))
                }
            } catch (e: Exception) {
                message.reply {
                    content = "Error: ${e.message}"
                }
            }
        }

        kord.on<ReactionAddEvent> {
            runCatching {
                if (emoji.name == JOIN_EMOJI && message.asMessage().author?.isBot == true) {
                    val callToArmsChannel = message.asMessage().callsToArms()
                    val user = user.asUser()
                    val currentPermissions = callToArmsChannel.asChannelOf<TextChannel>()
                        .getEffectivePermissions(user.id)
                    if (Permission.ViewChannel in currentPermissions) {
                        // User is already in the channel.
                        return@on
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
                    logger.debug { "User ${user.mention} reacted with $JOIN_EMOJI in ${message.channel.mention} with intent to join ${callToArmsChannel.mention}" }
                }
            }
        }

        kord.on<ReactionRemoveEvent> {
            runCatching {
                if (emoji.name == JOIN_EMOJI && message.asMessage().author?.isBot == true) {
                    val callToArmsChannel = message.asMessage().callsToArms()
                    val user = user.asUser()
                    val isChannelAdmin = Permission.ManageChannels in callToArmsChannel.asChannelOf<TextChannel>()
                        .getEffectivePermissions(user.id)
                    if (isChannelAdmin) {
                        // Admin can't leave this way.
                        return@on
                    }
                    callToArmsChannel
                        .asChannelOf<TextChannel>()
                        .editMemberPermission(user.id) {
                            reason = "Automation: user removed reaction to channel join call to arms"
                            if (Permission.ManageChannels !in allowed) {
                                allowed = Permissions()
                            }
                        }
                    logger.debug { "User ${user.mention} removed reaction of $JOIN_EMOJI in ${message.channel.mention} with intent to leave ${callToArmsChannel.mention}" }
                }
            }
        }
    }
}