package dev.mtib.ketchup.bot.features.openai.storage

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal

object GptTrackingTable : LongIdTable("gpt_tracking") {
    val userId = ulong("user_id")
    val messageId = ulong("message_id")
    val requestedAt = timestamp("requested_at")
    val prompt = text("prompt")
    val response = text("response").nullable()
    val cost = decimal("cost", 10, 4)
    val successful = bool("successful")
    val note = text("note").nullable()

    fun fail(userId: ULong, messageId: ULong, prompt: String, cost: BigDecimal, note: String? = null) {
        insert {
            it[GptTrackingTable.userId] = userId
            it[GptTrackingTable.messageId] = messageId
            it[GptTrackingTable.requestedAt] = java.time.Instant.now()
            it[GptTrackingTable.prompt] = prompt
            it[GptTrackingTable.cost] = cost
            it[GptTrackingTable.successful] = false
            it[GptTrackingTable.note] = note
        }
    }

    fun succeed(
        userId: ULong,
        messageId: ULong,
        prompt: String,
        response: String,
        cost: BigDecimal,
        note: String? = null
    ) {
        insert {
            it[GptTrackingTable.userId] = userId
            it[GptTrackingTable.messageId] = messageId
            it[GptTrackingTable.requestedAt] = java.time.Instant.now()
            it[GptTrackingTable.prompt] = prompt
            it[GptTrackingTable.response] = response
            it[GptTrackingTable.cost] = cost
            it[GptTrackingTable.successful] = true
            it[GptTrackingTable.note] = note
        }
    }
}