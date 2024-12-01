package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.reply
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.features.aoc.AocPoster
import dev.mtib.ketchup.bot.utils.ketchupZone
import dev.mtib.ketchup.bot.utils.now
import dev.mtib.ketchup.bot.features.aoc.Client as AocClient

object AocNowCommand : AdminCommand(
    commandName = "aocnow",
    commandShortDescription = "Post the current AoC leaderboard for all listeners subscribed to the current channel.",
    commandHelp = "Post the current AoC leaderboard for all listeners subscribed to the current channel.",
) {
    override suspend fun MessageCreateEvent.authorized(kord: Kord) {
        val day = ketchupZone.now().also {
            if (it.monthValue != 12) {
                message.reply {
                    content = "This command can only be used in December."
                }
                return
            }
            if (it.dayOfMonth > 26) {
                message.reply {
                    content = "This command can only be used between the 1st and 25th of December."
                }
                return
            }
        }.dayOfMonth

        val channel = this.message.channel.asChannelOfOrNull<TextChannel>()
        if (channel == null) {
            message.reply {
                content = "This command can only be used in a text channel."
            }
            return
        }

        AocClient.getListeners().filter { it.snowflake == channel.id.toString() }.forEach {
            val data = AocClient.getLeaderboard(it.event.toInt(), it.ownerId, it.cookie)
            AocPoster.post(channel, data, day)
        }
    }
}