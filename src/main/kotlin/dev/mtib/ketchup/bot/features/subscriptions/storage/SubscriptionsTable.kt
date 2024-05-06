package dev.mtib.ketchup.bot.features.subscriptions.storage

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant

object SubscriptionsTable : LongIdTable("subscriptions") {
    val ownerId = ulong("owner_id")
    val roleId = ulong("role_id")
    val createdAt = timestamp("created_at")

    fun getSubscriptionById(id: Long): SubscriptionDTO? {
        return SubscriptionsTable
            .select(SubscriptionsTable.id, ownerId, roleId, createdAt)
            .where(SubscriptionsTable.id eq id)
            .map {
                SubscriptionDTO.fromRow(it)
            }
            .firstOrNull()
    }

    fun getSubscriptionsByOwnerId(ownerId: ULong): List<SubscriptionDTO> {
        return SubscriptionsTable
            .select(SubscriptionsTable.id, this.ownerId, roleId, createdAt)
            .where(SubscriptionsTable.ownerId eq ownerId)
            .map {
                SubscriptionDTO.fromRow(it)
            }
    }

    fun getAllSubscriptions(): List<SubscriptionDTO> {
        return SubscriptionsTable
            .selectAll()
            .map {
                SubscriptionDTO.fromRow(it)
            }
    }

    fun getSubscriptionByRole(roleId: ULong): SubscriptionDTO? {
        return SubscriptionsTable
            .select(SubscriptionsTable.id, ownerId, this.roleId, createdAt)
            .where(SubscriptionsTable.roleId eq roleId)
            .map {
                SubscriptionDTO.fromRow(it)
            }
            .firstOrNull()
    }

    fun createSubscription(ownerId: ULong, roleId: ULong): SubscriptionDTO {
        val id = SubscriptionsTable.insertAndGetId {
            it[SubscriptionsTable.ownerId] = ownerId
            it[SubscriptionsTable.roleId] = roleId
            it[SubscriptionsTable.createdAt] = Instant.now()
        }
        return getSubscriptionById(id.value)!!
    }

    data class SubscriptionDTO(
        val id: Long,
        val ownerId: ULong,
        val roleId: ULong,
        val createdAt: Instant
    ) {
        companion object {
            fun fromRow(row: ResultRow): SubscriptionDTO {
                return SubscriptionDTO(
                    id = row[SubscriptionsTable.id].value,
                    ownerId = row[SubscriptionsTable.ownerId],
                    roleId = row[SubscriptionsTable.roleId],
                    createdAt = row[SubscriptionsTable.createdAt]
                )
            }
        }
    }
}