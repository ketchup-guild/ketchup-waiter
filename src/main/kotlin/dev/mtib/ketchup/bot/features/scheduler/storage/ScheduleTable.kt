package dev.mtib.ketchup.bot.features.scheduler.storage

import dev.kord.common.entity.Snowflake
import dev.kord.core.supplier.EntitySupplier
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.time.Instant

object ScheduleTable : LongIdTable("scheduled_messages") {
    val messageChannelId = ulong("message_channel_id")
    val messageId = ulong("message_id")
    val targetId = ulong("target_id")
    val time = timestamp("time")
    val createdAt = timestamp("created_at")
    val sentAt = timestamp("sent_at").nullable()
    val sent = bool("sent")

    init {
        index("scheduled_messages_message_id", true, messageId)
        index(
            "scheduled_messages_sent_time",
            false,
            sent,
            time
        )
    }

    private fun SqlExpressionBuilder.unsent() = sent eq false
    private fun SqlExpressionBuilder.sent() = sent eq true
    private fun SqlExpressionBuilder.due() = unsent().and { time lessEq Instant.now() }

    fun create(message: MessageIdentification, targetId: ULong, time: Instant) {
        ScheduleTable.insert {
            it[this.messageChannelId] = message.messageChannelId
            it[this.messageId] = message.messageId
            it[this.targetId] = targetId
            it[this.time] = time
            it[createdAt] = Instant.now()
            it[sentAt] = null
            it[sent] = false
        }
    }

    fun update2(updateMessageId: ULong, updateFun: ScheduleTable.(UpdateStatement) -> Unit) {
        ScheduleTable.update({ messageId eq updateMessageId }, body = updateFun)
    }

    fun markAsSent(sentMessageId: ULong) {
        ScheduleTable.update({ messageId eq sentMessageId }) {
            it[sent] = true
            it[sentAt] = Instant.now()
        }
    }

    fun getUnsentMessages() = ScheduleTable.selectAll().where { unsent() }.mapLazy { row ->
        Schedule.fromResultRow(row)
    }

    fun getDueMessages() = ScheduleTable.selectAll().where { due() }.mapLazy {
        Schedule.fromResultRow(it)
    }

    data class Schedule(
        val messageIdentification: MessageIdentification,
        val targetId: ULong,
        val time: Instant,
        val createdAt: Instant,
        val sent: Boolean,
        val sentAt: Instant?
    ) {
        companion object {
            fun fromResultRow(row: ResultRow) = Schedule(
                messageIdentification = MessageIdentification(
                    messageId = row[messageId],
                    messageChannelId = row[messageChannelId]
                ),
                targetId = row[targetId],
                time = row[time],
                createdAt = row[createdAt],
                sent = row[sent],
                sentAt = row[sentAt],
            )
        }

        val messageSnowflake: Snowflake
            get() = messageIdentification.messageSnowflake
        val messageChannelSnowflake: Snowflake
            get() = messageIdentification.messageChannelSnowflake
        val targetSnowflake: Snowflake
            get() = Snowflake(targetId)
    }

    data class MessageIdentification(
        val messageId: ULong,
        val messageChannelId: ULong
    ) {
        val messageSnowflake: Snowflake
            get() = Snowflake(messageId)
        val messageChannelSnowflake: Snowflake
            get() = Snowflake(messageChannelId)

        suspend fun getFromSupplier(entitySupplier: EntitySupplier) =
            entitySupplier.getMessage(messageChannelSnowflake, messageSnowflake)
    }
}