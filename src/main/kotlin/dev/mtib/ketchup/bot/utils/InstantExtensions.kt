package dev.mtib.ketchup.bot.utils

import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.toMessageFormat
import kotlinx.datetime.toKotlinInstant
import java.time.Instant as JavaInstant

fun JavaInstant.toMessageFormat(
    style: DiscordTimestampStyle? = null
): String = this.toKotlinInstant().toMessageFormat(style)