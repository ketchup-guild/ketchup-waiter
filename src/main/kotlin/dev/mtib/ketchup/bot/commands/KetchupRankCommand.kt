package dev.mtib.ketchup.bot.commands

import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.features.ketchupRank.storage.KetchupRankRepository
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros
import mu.KotlinLogging

class KetchupRankCommand() : ChannelCommand(
    "rank",
    "Ketchup Rank",
    "Shows the ketchup bottle ranking of the guild",
) {
    private val logger = KotlinLogging.logger { }
    override val completeness: Completeness = Completeness.Complete

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val channel = message.getChannel().asChannelOf<TextChannel>()
        val isAdminChannel = channel.name in setOf("bot", "system")
        val rankings = KetchupRankRepository.getRanking(kord, if (isAdminChannel) null else channel)
        message.reply {
            content = buildString {
                append("## Ketchup ranking:\n")
                if (rankings.isEmpty()) {
                    appendLine("No one has any ketchup yet!")
                    return@buildString
                }
                rankings.forEachIndexed { index, user ->
                    val userName = message.getGuild().getMember(user.user.id).effectiveName
                    appendLine("${index + 1}. **${userName}** - `${user.score.stripTrailingFractionalZeros()}` bottles of ketchup")
                }
                appendLine("_(only shows users with access to ${message.getChannel().mention})_")
            }
        }
    }
}
