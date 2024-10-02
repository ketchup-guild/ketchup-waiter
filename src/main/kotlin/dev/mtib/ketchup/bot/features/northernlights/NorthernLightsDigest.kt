package dev.mtib.ketchup.bot.features.northernlights

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.bot.features.northernlights.models.Forecast
import dev.mtib.ketchup.bot.utils.getEnv
import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import mu.KotlinLogging
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

object NorthernLightsDigest : Feature {
    private lateinit var job: Job
    private val logger = KotlinLogging.logger { }

    override fun cancel() {
        job.cancel()
    }

    override fun register(kord: Kord) {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                run(kord)
                delay(determineSleepTime().toJavaDuration())
            }
        }
    }

    private fun determineSleepTime(): Duration {
        val now = Clock.System.now().toJavaInstant()
        val next6PM =
            ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).withHour(18).withMinute(0).withSecond(0).withNano(0)!!
        return (
                if (now.isAfter(next6PM.toInstant())) {
                    next6PM.plusDays(1)
                } else {
                    next6PM
                }
                    .toInstant()
                    .toEpochMilli() - now.toEpochMilli()
                ).milliseconds
    }

    private val defaultChannel = Snowflake("1285315457032913027")

    sealed class RunResult(
        val forecast: Forecast,
        val score: ScoreCalculator.Score,
    ) {
        class Posted(
            forecast: Forecast,
            score: ScoreCalculator.Score,
            val messages: List<String>,
            val channelId: Snowflake,
            val messageId: Snowflake,
        ) : RunResult(
            forecast,
            score
        )

        class Aborted(
            forecast: Forecast,
            score: ScoreCalculator.Score,
        ) : RunResult(
            forecast,
            score
        )
    }

    suspend fun run(kord: Kord, channelSnowflake: Snowflake = defaultChannel): RunResult {
        val data = Client.get3DayForecast()
        val score = ScoreCalculator.score(data)

        if (!score.interesting) {
            logger.info {
                "Not posting Northern Lights forecast because it's not interesting: geomagnetic=${score.geomagnetic}, radiation=${score.radiation}"
            }
            return RunResult.Aborted(data, score)
        } else {
            logger.info {
                "Posting Northern Lights forecast because it's interesting: geomagnetic=${score.geomagnetic}, radiation=${score.radiation}"
            }
        }

        val channel = kord.getChannelOf<TextChannel>(channelSnowflake)!!

        val messageData = mutableListOf<String>()
        val message = channel.createMessage(buildString {
            appendLine("# Northern Lights Forecast")
            appendLine()
            appendLine(data.comment)
            appendLine()
            data.metadata.forEach { (key, value) ->
                appendLine("- $key: $value")
            }
        }.also { messageData.add(it) })

        val thread = channel.startPublicThreadWithMessage(message.id, data.metadata["Issued"] ?: "Details")
        data.sections.forEach {
            thread.createMessage(buildString {
                appendLine("## ${it.title}")
                appendLine(it.markdownContent)
            }.also { messageData.add(it) })
        }

        return RunResult.Posted(data, score, messageData, channelSnowflake, message.id)
    }
}

suspend fun main(): Unit = coroutineScope {
    val kord = Kord(getEnv("KETCHUP_BOT_TOKEN"))

    NorthernLightsDigest.run(kord)
}
