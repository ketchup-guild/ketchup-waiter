package dev.mtib.ketchup.bot.utils

import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.TopGuildChannelBehavior
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Category
import dev.kord.core.entity.channel.TextChannel
import dev.mtib.ketchup.bot.commands.Command
import dev.mtib.ketchup.bot.storage.Storage

val User?.isGod: Boolean
    get() = this?.id in getAnywhere<Storage.Gods>()

fun Message.matchesSignature(command: Command): Boolean {
    if (author?.isBot != false) {
        return false
    }
    return content.startsWith(command.prefix)
}

fun Message.getCommandArgs(command: Command): List<String> {
    if (!matchesSignature(command)) {
        return emptyList()
    }
    if (content == command.prefix) {
        return emptyList()
    }
    return content.removePrefix(command.prefix).trim().split(Regex("\\s+"))
}

fun Message.getCommandBody(command: Command): String {
    if (!matchesSignature(command)) {
        throw IllegalArgumentException("Message does not match command signature")
    }
    return content.removePrefix(command.prefix).trim()
}

suspend fun Guild.getCategoryByNameOrNull(name: String): TopGuildChannelBehavior? {
    return channelBehaviors.find {
        val category = it.asChannelOfOrNull<Category>()
        if (category != null) {
            category.name.lowercase() == name.lowercase()
        } else {
            false
        }
    }
}

suspend fun Guild.createPrivateChannelFor(
    name: String,
    description: String,
    adminUser: User,
    categoryId: Snowflake,
    giveChannelAdmin: Boolean = false,
): TextChannel {
    return createTextChannel(name) {
        topic = description
        reason = "Automation"
        parentId = categoryId
        permissionOverwrites = buildSet {
            add(
                Overwrite(
                    id = everyoneRole.id,
                    type = OverwriteType.Role,
                    deny = Permissions(Permission.ViewChannel),
                    allow = Permissions(),
                )
            )
            add(
                Overwrite(
                    id = adminUser.id,
                    type = OverwriteType.Member,
                    allow = Permissions(
                        buildList {
                            add(Permission.ViewChannel)
                            if (giveChannelAdmin) {
                                addAll(
                                    listOf(
                                        Permission.ManageMessages,
                                        Permission.ManageChannels,
                                        Permission.ManageRoles,
                                    )
                                )
                            }
                        } as Iterable<Permission>,
                    ),
                    deny = Permissions(),
                )
            )
        }.toMutableSet()
    }
}

fun messageLink(
    guildId: Snowflake,
    channelId: Snowflake,
    messageId: Snowflake,
): String {
    return "https://discord.com/channels/$guildId/$channelId/$messageId"
}

fun messageLink(
    guild: Guild,
    channel: TextChannel,
    message: Message,
): String {
    return messageLink(guild.id, channel.id, message.id)
}