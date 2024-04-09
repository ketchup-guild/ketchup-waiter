package dev.mtib.ketchup.bot.utils

import java.math.BigDecimal

fun BigDecimal.stripTrailingFractionalZeros(): BigDecimal {
    val fullyStripped = this.stripTrailingZeros()
    if (fullyStripped.scale() < 0) {
        return fullyStripped.setScale(0)
    }
    return fullyStripped
}