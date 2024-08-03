package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.DmChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.mtib.ketchup.bot.features.ketchupRank.storage.KetchupRankRepository
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros

object Rank : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PUBLIC
    override val name: String = "rank"
    override val description: String = "Shows the ketchup bottle ranking of the guild"

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()
        val channel = event.interaction.channel.asChannel()
        val rankings = KetchupRankRepository.getRanking(kord, channel)
        response.respond {
            content = buildString {
                append("## Ketchup ranking:\n")
                if (rankings.isEmpty()) {
                    appendLine("No one has any ketchup yet!")
                    return@buildString
                }
                rankings.forEach { ranking ->
                    val userName = when (channel) {
                        is TextChannel -> {
                            channel.guild.getMember(ranking.user.id).effectiveName
                        }

                        is DmChannel -> ranking.user.mention
                        else -> ranking.user.effectiveName
                    }
                    appendLine("${ranking.position}. **${userName}** - `${ranking.score.stripTrailingFractionalZeros()}` bottles of ketchup")
                }
            }
        }
    }

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.dmPermission = true
    }
}