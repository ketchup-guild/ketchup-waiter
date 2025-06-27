package dev.mtib.ketchup.bot.utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
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