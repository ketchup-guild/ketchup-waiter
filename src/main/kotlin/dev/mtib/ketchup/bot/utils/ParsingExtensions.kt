package dev.mtib.ketchup.bot.utils

import java.time.LocalDate

data class KetchupDate(val year: Int, val month: Int, val day: Int) {
    companion object {
        fun String.toKetchupDate(): KetchupDate {
            if (!matches(Regex("\\d{4}-\\d{1,2}-\\d{1,2}"))) throw IllegalArgumentException("Invalid date format")
            val split = split("-")
            return KetchupDate(split[0].toInt(), split[1].toInt(), split[2].toInt())
        }

        fun String.toKetchupDateOrNull(): KetchupDate? {
            return try {
                toKetchupDate()
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toLocalDate(): LocalDate {
        return LocalDate.of(year, month, day)
    }

    fun LocalDate.toKetchupDate(): KetchupDate {
        return KetchupDate(year, monthValue, dayOfMonth)
    }

    override fun toString(): String {
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }
}