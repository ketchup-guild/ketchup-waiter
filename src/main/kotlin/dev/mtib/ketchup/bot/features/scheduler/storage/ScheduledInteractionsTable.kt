package dev.mtib.ketchup.bot.features.scheduler.storage

import arrow.core.Option
import arrow.core.Some
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.mtib.ketchup.bot.features.scheduler.entities.Target
import dev.mtib.ketchup.bot.features.scheduler.interfaces.ScheduledAction
import dev.mtib.ketchup.bot.features.scheduler.interfaces.ScheduledAction.ActionNotDueException.InteractionNotDueException
import dev.mtib.ketchup.bot.utils.toMessageFormat
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.javatime.timestampLiteral
import java.time.Instant

object ScheduledInteractionsTable : LongIdTable("scheduled_interactions") {
    val messageChannelId = ulong("message_channel_id")
    val sendingUserId = ulong("sending_user_id")
    val message = text("message")
    val time = timestamp("time")
    val createdAt = timestamp("created_at")
    val sentAt = timestamp("sent_at").nullable()
    val sent = bool("sent")

    fun create(messageChannelId: ULong, sendingUserId: ULong, message: String, time: Instant) {
        ScheduledInteractionsTable.insert {
            it[this.messageChannelId] = messageChannelId
            it[this.sendingUserId] = sendingUserId
            it[this.message] = message
            it[this.time] = time
            it[createdAt] = Instant.now()
            it[sentAt] = null
            it[sent] = false
        }
    }

    fun markAsSent(sentMessageId: EntityID<Long>) {
        ScheduledInteractionsTable.update({ id eq sentMessageId }) {
            it[sent] = true
            it[sentAt] = Instant.now()
        }
    }

    fun getUnsentMessages() = ScheduledInteractionsTable
        .selectAll()
        .where { sent eq false }
        .mapLazy { row ->
            ScheduledInteraction.fromResultRow(row)
        }

    fun getDueUnsentMessages() = ScheduledInteractionsTable
        .selectAll()
        .where {
            (sent eq false)
                .and(time.lessEq(timestampLiteral(Instant.now()).castTo(time.columnType)))
        }
        .mapLazy { row ->
            ScheduledInteraction.fromResultRow(row)
        }

    data class ScheduledInteraction(
        val id: EntityID<Long>,
        val messageChannelId: ULong,
        val sendingUserId: ULong,
        val message: String,
        val time: Instant,
        val createdAt: Instant,
        val sentAt: Instant?,
        val sent: Boolean
    ) : ScheduledAction {
        companion object {
            fun fromResultRow(row: ResultRow): ScheduledInteraction {
                return ScheduledInteraction(
                    id = row[ScheduledInteractionsTable.id],
                    messageChannelId = row[ScheduledInteractionsTable.messageChannelId],
                    sendingUserId = row[ScheduledInteractionsTable.sendingUserId],
                    message = row[ScheduledInteractionsTable.message],
                    time = row[ScheduledInteractionsTable.time],
                    createdAt = row[ScheduledInteractionsTable.createdAt],
                    sentAt = row[ScheduledInteractionsTable.sentAt],
                    sent = row[ScheduledInteractionsTable.sent]
                )
            }
        }

        override suspend fun handleMessage(kord: Kord): Option<Transaction.() -> Unit> {
            if (!actionDue()) {
                throw InteractionNotDueException(this)
            }

            val target = Target.fromULong(kord, messageChannelId)
            val sender = kord.getUser(Snowflake(sendingUserId))

            target.send(
                """
                ${sender?.mention ?: "Someone"} scheduled this message ${createdAt.toMessageFormat(DiscordTimestampStyle.RelativeTime)}:
                
                $message
                """.trimIndent()
            )

            return Some {
                markAsSent(this@ScheduledInteraction.id)
            }
        }

        override fun actionDue(): Boolean = time.isBefore(Instant.now())
    }
}