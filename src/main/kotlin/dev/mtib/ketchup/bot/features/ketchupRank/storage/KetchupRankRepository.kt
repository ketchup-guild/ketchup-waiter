package dev.mtib.ketchup.bot.features.ketchupRank.storage

import arrow.fx.coroutines.mapIndexed
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.mtib.ketchup.bot.features.ketchupRank.entities.Rank
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import java.math.BigDecimal

object KetchupRankRepository {
    suspend fun getRanking(kord: Kord, limitToChannel: MessageChannel? = null): List<Rank> {
        val users = getAnywhere<Database>().transaction {
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

        return users.asFlow()
            .mapNotNull {
                when (val user = kord.getUser(Snowflake(it.userId))) {
                    null -> null
                    else -> object {
                        val user: User = user
                        val score: BigDecimal = it.score
                    }
                }
            }
            .let {
                when (limitToChannel) {
                    null -> it
                    is TextChannel -> it.filter {
                        limitToChannel.getEffectivePermissions(it.user.id)
                            .contains(Permission.ViewChannel)
                    }

                    else -> it
                }
            }
            .filter { it.user.id.value != 1118847235740946534uL }
            .filter { it.score.compareTo("0.0".toBigDecimal()) > 0 }
            .mapIndexed { index, it -> Rank(it.user, it.score, index + 1) }
            .toList()
    }
}