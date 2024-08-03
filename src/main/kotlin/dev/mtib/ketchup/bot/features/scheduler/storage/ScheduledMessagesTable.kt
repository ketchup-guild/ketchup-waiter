package dev.mtib.ketchup.bot.features.scheduler.storage

import arrow.core.Option
import arrow.core.Some
import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.supplier.EntitySupplier
import dev.mtib.ketchup.bot.commands.schedule.ScheduleMessageCommand
import dev.mtib.ketchup.bot.features.scheduler.entities.Target
import dev.mtib.ketchup.bot.features.scheduler.interfaces.ScheduledAction
import dev.mtib.ketchup.bot.features.scheduler.interfaces.ScheduledAction.ActionNotDueException.MessageNotDueException
import dev.mtib.ketchup.bot.utils.toMessageFormat
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.time.Instant

object ScheduledMessagesTable : LongIdTable("scheduled_messages") {
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
        ScheduledMessagesTable.insert {
            it[this.messageChannelId] = message.messageChannelId
            it[this.messageId] = message.messageId
            it[this.targetId] = targetId
            it[this.time] = time
            it[createdAt] = Instant.now()
            it[sentAt] = null
            it[sent] = false
        }
    }

    fun update2(updateMessageId: ULong, updateFun: ScheduledMessagesTable.(UpdateStatement) -> Unit) {
        ScheduledMessagesTable.update({ messageId eq updateMessageId }, body = updateFun)
    }

    fun markAsSent(sentMessageId: ULong) {
        ScheduledMessagesTable.update({ messageId eq sentMessageId }) {
            it[sent] = true
            it[sentAt] = Instant.now()
        }
    }

    fun getUnsentMessages() = ScheduledMessagesTable.selectAll().where { unsent() }.mapLazy { row ->
        ScheduledMessage.fromResultRow(row)
    }

    fun getDueMessages() = ScheduledMessagesTable.selectAll().where { due() }.mapLazy {
        ScheduledMessage.fromResultRow(it)
    }

    data class ScheduledMessage(
        val messageIdentification: MessageIdentification,
        val targetId: ULong,
        val time: Instant,
        val createdAt: Instant,
        val sent: Boolean,
        val sentAt: Instant?
    ) : ScheduledAction {
        companion object {
            fun fromResultRow(row: ResultRow) = ScheduledMessage(
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

        override suspend fun handleMessage(kord: Kord): Option<Transaction.() -> Unit> {
            val target = Target.fromULong(kord, targetId)
            val source = messageIdentification.getFromSupplier(kord.defaultSupplier)

            val specs = ScheduleMessageCommand.extractMessageSpecs(source)

            val correctionSteps = buildList<ScheduledMessagesTable.(UpdateStatement) -> Unit> {
                if (specs.target.value != targetId) {
                    add {
                        it[targetId] = specs.target.value
                    }
                }
                if (specs.time != time) {
                    add {
                        it[time] = specs.time
                    }
                }
            }

            if (correctionSteps.isNotEmpty()) {
                return Some {
                    update2(messageIdentification.messageId) {
                        correctionSteps.forEach { correction ->
                            this.correction(it)
                        }
                    }
                }
            }

            if (!actionDue()) {
                throw MessageNotDueException(this)
            }

            target.send(
                """
                ${specs.content}
                
                > Scheduled by ${source.author!!.mention} in ${source.channel.mention} ${
                    createdAt.toMessageFormat(
                        DiscordTimestampStyle.RelativeTime
                    )
                } to be sent ${time.toMessageFormat(DiscordTimestampStyle.RelativeTime)}
                """.trimIndent()
            )

            return Some {
                ScheduledMessagesTable.markAsSent(messageIdentification.messageId)
            }
        }

        override fun actionDue(): Boolean = time.isBefore(Instant.now())
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