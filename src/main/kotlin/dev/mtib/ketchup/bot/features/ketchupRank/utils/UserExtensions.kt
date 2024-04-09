package dev.mtib.ketchup.bot.features.ketchupRank.utils

import dev.kord.core.entity.User
import dev.mtib.ketchup.bot.features.ketchupRank.KetchupRankTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import java.math.BigDecimal

fun User.hasAtLeastKetchup(amount: Int): Boolean {
    val db = getAnywhere<Database>()
    return db.transaction {
        val ketchupUser = KetchupRankTable.safeGet(this@hasAtLeastKetchup)
        ketchupUser.ketchupCount.compareTo(amount.toBigDecimal()) >= 0
    }
}

sealed class KetchupPaymentResult {
    fun asSuccess(): KetchupPaymentSuccess = this as KetchupPaymentSuccess
    fun asFailure(): KetchupPaymentFailure = this as KetchupPaymentFailure
}

class KetchupPaymentSuccess(
    val remainingKetchup: BigDecimal,
    val paidKetchup: BigDecimal,
) : KetchupPaymentResult()

class KetchupPaymentFailure(
    val remainingKetchup: BigDecimal,
    val requestedKetchup: BigDecimal,
    val reason: String,
) : KetchupPaymentResult()

fun User.payKetchup(amount: BigDecimal): KetchupPaymentResult {
    val db = getAnywhere<Database>()
    return db.transaction {
        val ketchupUser = KetchupRankTable.safeGet(this@payKetchup)
        if (ketchupUser.ketchupCount.compareTo(amount) < 0) {
            return@transaction KetchupPaymentFailure(ketchupUser.ketchupCount, amount, "Not enough ketchup")
        }
        val newBalance = ketchupUser.ketchupCount - amount
        KetchupRankTable.updateUser(this@payKetchup) {
            it[ketchupCount] = newBalance
        }
        KetchupPaymentSuccess(
            newBalance,
            amount
        )
    }
}

fun User.refundKetchup(amount: BigDecimal) {
    val db = getAnywhere<Database>()
    db.transaction {
        val ketchupUser = KetchupRankTable.safeGet(this@refundKetchup)
        KetchupRankTable.updateUser(this@refundKetchup) {
            it[ketchupCount] = ketchupUser.ketchupCount + amount
        }
    }
}