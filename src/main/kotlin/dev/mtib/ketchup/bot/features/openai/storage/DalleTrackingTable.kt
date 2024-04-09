package dev.mtib.ketchup.bot.features.openai.storage

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal

object DalleTrackingTable : LongIdTable("dalle_tracking") {
    val userId = ulong("user_id")
    val messageId = ulong("message_id")
    val requestedAt = timestamp("requested_at")
    val prompt = text("prompt")
    val responseUrl = text("response_url").nullable()
    val cost = decimal("cost", 10, 4)
    val successful = bool("successful")
    val note = text("note").nullable()

    fun fail(userId: ULong, messageId: ULong, prompt: String, cost: BigDecimal, note: String? = null) {
        insert {
            it[DalleTrackingTable.userId] = userId
            it[DalleTrackingTable.messageId] = messageId
            it[DalleTrackingTable.requestedAt] = java.time.Instant.now()
            it[DalleTrackingTable.prompt] = prompt
            it[DalleTrackingTable.cost] = cost
            it[DalleTrackingTable.successful] = false
            it[DalleTrackingTable.note] = note
        }
    }

    fun succeed(
        userId: ULong,
        messageId: ULong,
        prompt: String,
        responseUrl: String,
        cost: BigDecimal,
        note: String? = null
    ) {
        insert {
            it[DalleTrackingTable.userId] = userId
            it[DalleTrackingTable.messageId] = messageId
            it[DalleTrackingTable.requestedAt] = java.time.Instant.now()
            it[DalleTrackingTable.prompt] = prompt
            it[DalleTrackingTable.responseUrl] = responseUrl
            it[DalleTrackingTable.cost] = cost
            it[DalleTrackingTable.successful] = true
            it[DalleTrackingTable.note] = note
        }
    }
}