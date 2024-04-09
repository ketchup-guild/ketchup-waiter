package dev.mtib.ketchup.bot.features.ketchupRank.storage

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp

object KetchupGivingTable : LongIdTable("ketchup_giving") {
    val giverId = ulong("giver_id").references(KetchupRankTable.userId)
    val receiverId = ulong("receiver_id").references(KetchupRankTable.userId)
    val givenAt = timestamp("given_at")
    val amount = integer("amount")
    val reason = text("reason")
    val messageId = ulong("message_id")

    fun create(
        giverId: ULong,
        receiverId: ULong,
        givenAt: java.time.Instant,
        amount: Int,
        reason: String,
        messageId: ULong
    ) {
        insert {
            it[KetchupGivingTable.giverId] = giverId
            it[KetchupGivingTable.receiverId] = receiverId
            it[KetchupGivingTable.givenAt] = givenAt
            it[KetchupGivingTable.amount] = amount
            it[KetchupGivingTable.reason] = reason
            it[KetchupGivingTable.messageId] = messageId
        }
    }
}