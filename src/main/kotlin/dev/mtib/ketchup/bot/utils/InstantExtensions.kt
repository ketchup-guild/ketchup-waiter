package dev.mtib.ketchup.bot.utils

import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds
import java.time.Instant as JavaInstant

fun JavaInstant.toMessageFormat(
    style: DiscordTimestampStyle? = DiscordTimestampStyle.RelativeTime
): String = this.toKotlinInstant().toMessageFormat(style)

fun ZoneId.now() = Clock.System.now().toJavaInstant().atZone(this)!!

fun ZonedDateTime.toKotlinInstant() = this.toInstant().toKotlinInstant()

fun ZonedDateTime.nextClockTime(hours: Int, minutes: Int = 0): ZonedDateTime {
    val now = this
    val next = now.withHour(hours).withMinute(minutes).withSecond(0).withNano(0)
    return if (now.isAfter(next)) next.plusDays(1) else next
}

fun CoroutineScope.launchAtClockTime(
    zone: ZoneId,
    hours: Int,
    minutes: Int = 0,
    block: suspend CoroutineScope.() -> Unit
): Job = launch {
    while (isActive) {
        val now = zone.now()
        val next = now.nextClockTime(hours, minutes)
        val sleepDuration = now.until(next, ChronoUnit.SECONDS).seconds
        delay(sleepDuration)
        block()
    }
}