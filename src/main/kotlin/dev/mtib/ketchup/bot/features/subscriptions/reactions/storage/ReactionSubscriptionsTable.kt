package dev.mtib.ketchup.bot.features.subscriptions.reactions.storage

import arrow.core.singleOrNone
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.ulongLiteral
import org.jetbrains.exposed.sql.upsert

object ReactionSubscriptionsTable : LongIdTable("reaction_subscriptions") {
    private val userId = ulong("user_id").uniqueIndex()

    fun isSubscribed(userId: ULong): Boolean {
        return ReactionSubscriptionsTable
            .select(ReactionSubscriptionsTable.id)
            .where(ReactionSubscriptionsTable.userId eq userId)
            .singleOrNone()
            .isSome()
    }

    fun subscribe(userId: ULong) {
        ReactionSubscriptionsTable.upsert(
            ReactionSubscriptionsTable.userId,
            onUpdate = listOf(
                ReactionSubscriptionsTable.userId to ulongLiteral(userId)
            ),
            where = {
                ReactionSubscriptionsTable.userId eq userId
            }
        ) {
            it[ReactionSubscriptionsTable.userId] = userId
        }
    }

    fun unsubscribe(userId: ULong) {
        ReactionSubscriptionsTable.deleteWhere {
            ReactionSubscriptionsTable.userId eq userId
        }
    }
}