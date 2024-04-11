package dev.mtib.ketchup.bot.features.ketchupRank.storage

import dev.kord.core.entity.User
import dev.mtib.ketchup.bot.features.ketchupRank.KetchupRank.Companion.DAILY_KETCHUP_AMOUNT
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


object KetchupRankTable : LongIdTable("ketchup_rank") {
    val userId = ulong("user_id").uniqueIndex("user_id_unique_index")
    val ketchupCount = decimal("ketchup_count", 12, 4).default("0.0".toBigDecimal())
    val ketchupReset = timestamp("ketchup_reset").nullable()
    val ketchupRemaining = integer("ketchup_remaining").default(DAILY_KETCHUP_AMOUNT)

    fun getOrCreate(userId: ULong): KetchupRankDTO {
        val result = KetchupRankTable
            .select(KetchupRankTable.id)
            .where(KetchupRankTable.userId eq userId)
            .count()
        if (result == 0L) {
            KetchupRankTable.insert {
                it[KetchupRankTable.userId] = userId
            }
        }
        return KetchupRankTable
            .select(KetchupRankTable.userId, ketchupCount, ketchupReset, ketchupRemaining)
            .where(KetchupRankTable.userId eq userId)
            .map {
                it.toKetchupRankDTO()
            }
            .first()
    }

    fun getOrCreate(user: User) = getOrCreate(user.id.value)

    fun reset(userId: ULong) {
        val resetTime = Instant.now().atZone(ZoneId.of("UTC"))
            .plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
        val databaseLiteralFormat = "uuuu-MM-dd HH:mm:ss.SSS"
        KetchupRankTable.upsert(
            KetchupRankTable.userId,
            onUpdate = listOf(
                KetchupRankTable.ketchupReset to stringLiteral(
                    DateTimeFormatter.ofPattern(databaseLiteralFormat).format(resetTime)
                ),
                KetchupRankTable.ketchupRemaining to intLiteral(DAILY_KETCHUP_AMOUNT),
            ),
            where = { KetchupRankTable.userId eq userId }
        ) {
            it[KetchupRankTable.userId] = userId
            it[KetchupRankTable.ketchupReset] = resetTime.toInstant()
            it[KetchupRankTable.ketchupRemaining] = DAILY_KETCHUP_AMOUNT
            it[KetchupRankTable.ketchupCount] = BigDecimal.ZERO
        }
    }

    fun reset(user: User) = reset(user.id.value)

    fun updateUser(userId: ULong, body: KetchupRankTable.(it: UpdateStatement) -> Unit) =
        update({ KetchupRankTable.userId eq userId }, body = body)

    fun updateUser(user: User, body: KetchupRankTable.(it: UpdateStatement) -> Unit) = updateUser(user.id.value, body)

    fun safeGet(userId: ULong): KetchupRankDTO {
        getOrCreate(userId).let {
            if (it.ketchupReset == null || it.ketchupReset.isBefore(Instant.now())) {
                reset(userId)
            }
        }
        return getOrCreate(userId)
    }

    fun safeGet(user: User) = safeGet(user.id.value)

    fun ResultRow.toKetchupRankDTO(): KetchupRankDTO = KetchupRankDTO(
        this[userId],
        this[ketchupCount],
        this[ketchupReset],
        this[ketchupRemaining]
    )

    data class KetchupRankDTO(
        val userId: ULong,
        val ketchupCount: BigDecimal,
        val ketchupReset: Instant?,
        val ketchupRemaining: Int
    )
}