package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.number
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getDoubleOptionByName
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getLongOptionByName
import dev.mtib.ketchup.bot.utils.ketchupZone
import dev.mtib.ketchup.bot.utils.now
import kotlin.time.Duration.Companion.milliseconds
import dev.mtib.ketchup.bot.features.aoc.Client as AocClient

object ReportBenchmark : Interaction {
    override val visibility: Interaction.Companion.Visibility
        get() = Interaction.Companion.Visibility.PRIVATE
    override val name: String
        get() = "aoc_benchmark"
    override val description: String
        get() = "Report a benchmark result"

    private const val DAY_VAR = "part_1"
    private const val PART_VAR = "part_2"
    private const val TIME_MS_VAR = "part_3"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        super.build(it)
        it.integer(DAY_VAR, "The day of the benchmark") {
            Locale.ALL.forEach { locale ->
                this.name(locale, "day")
            }
        }
        it.integer(PART_VAR, "The part of the benchmark") {
            Locale.ALL.forEach { locale ->
                this.name(locale, "part")
            }
        }
        it.number(TIME_MS_VAR, "The time of the benchmark in milliseconds") {
            Locale.ALL.forEach { locale ->
                this.name(locale, "time_milliseconds")
            }
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val r = event.defer()

        val day = event.interaction.getLongOptionByName(DAY_VAR)!!.toInt()
        val part = event.interaction.getLongOptionByName(PART_VAR)!!.toInt()
        val timeMs = event.interaction.getDoubleOptionByName(TIME_MS_VAR)!!

        if (day !in 1..25 || part !in 1..2) {
            r.respond { content = "Invalid day or part" }
            return
        }

        if (day > ketchupZone.now().dayOfMonth) {
            r.respond { content = "Day is in the future" }
            return
        }

        AocClient.recordBenchmarkResult(
            event.interaction.user.id.value.toString(),
            ketchupZone.now().year.toString(),
            day,
            part,
            timeMs
        )

        val duration = timeMs.milliseconds

        r.respond { content = "Reported benchmark for day $day part $part: $duration" }
    }
}