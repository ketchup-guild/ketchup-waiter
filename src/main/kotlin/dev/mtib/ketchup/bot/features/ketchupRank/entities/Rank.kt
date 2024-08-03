package dev.mtib.ketchup.bot.features.ketchupRank.entities

import dev.kord.core.entity.User
import java.math.BigDecimal

data class Rank(val user: User, val score: BigDecimal, val position: Int)