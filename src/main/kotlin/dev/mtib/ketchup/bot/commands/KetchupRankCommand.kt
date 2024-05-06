package dev.mtib.ketchup.bot.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.features.ketchupRank.storage.KetchupRankTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import java.math.BigDecimal

class KetchupRankCommand() : ChannelCommand(
    "rank",
    "Ketchup Rank",
    "Shows the ketchup bottle ranking of the guild",
) {
    private val logger = KotlinLogging.logger { }
    override val completeness: Completeness = Completeness.Complete

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val db = getAnywhere<Database>()
        val users = db.transaction {
            KetchupRankTable
                .select(KetchupRankTable.userId, KetchupRankTable.ketchupCount)
                .sortedByDescending { it[KetchupRankTable.ketchupCount] }
                .map {
                    object {
                        val userId = it[KetchupRankTable.userId];
                        val score = it[KetchupRankTable.ketchupCount]
                    }
                }
        }
        val channel = message.getChannel().asChannelOf<TextChannel>()
        val isAdminChannel = channel.name in setOf("bot", "system")
        val userMentions = users.asFlow()
            .map {
                val user = kord.getUser(Snowflake(it.userId))
                if (user == null) {
                    logger.warn { "User with id ${it.userId} not found" }
                    return@map null
                }
                object {
                    val user: User = user
                    val score: BigDecimal = it.score
                }
            }
            .filterNotNull()
            .filter {
                isAdminChannel ||
                        channel.getEffectivePermissions(it.user.id)
                            .contains(Permission.ViewChannel)
            }
            .filter { it.user.id.value != 1118847235740946534uL }
            .filter { it.score.compareTo("0.0".toBigDecimal()) > 0 }
            .toList()

        message.reply {
            content = buildString {
                append("## Ketchup ranking:\n")
                if (userMentions.isEmpty()) {
                    appendLine("No one has any ketchup yet!")
                    return@buildString
                }
                userMentions.forEachIndexed { index, user ->
                    val userName = message.getGuild().getMember(user.user.id).effectiveName
                    appendLine("${index + 1}. **${userName}** - `${user.score.stripTrailingFractionalZeros()}` bottles of ketchup")
                }
                appendLine("_(only shows users with access to ${message.getChannel().mention})_")
            }
        }
    }
}