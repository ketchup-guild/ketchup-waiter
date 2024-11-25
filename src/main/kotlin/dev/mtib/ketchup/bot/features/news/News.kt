package dev.mtib.ketchup.bot.features.news

import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.TextChannel
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.bot.features.planner.storage.Locations
import dev.mtib.ketchup.bot.utils.ketchupZone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds

object News : Feature {
    private lateinit var job: Job

    private val logger = KotlinLogging.logger { }
    private const val DAY_OF_MONTH = 15
    private const val CET_HOUR_OF_DAY = 16

    private var nextExecution: ZonedDateTime? = null

    fun getNextExecution(): ZonedDateTime {
        synchronized(this) {
            return nextExecution!!
        }
    }

    override fun register(kord: Kord) {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (coroutineContext.isActive) {
                val now = ZonedDateTime.now(ketchupZone)
                val date = now.withDayOfMonth(DAY_OF_MONTH).withHour(CET_HOUR_OF_DAY).withMinute(0).withSecond(0)
                val nextRun = if (now.isAfter(date)) {
                    date.plusMonths(1)
                } else {
                    date
                }
                synchronized(this) {
                    nextExecution = nextRun
                }
                val d = (nextRun.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()).milliseconds
                delay(d)
                kord.getChannelOf<TextChannel>(Locations.fromEnvironment().ideaChannelSnowflake)?.let {
                    run(it)
                }
            }
        }
    }

    override fun cancel() {
        super.cancel()
        job.cancel()
    }

    suspend fun run(channel: TextChannel) {
        logger.debug { "Generating news digest..." }
        val data = Client.fetchNews()

        val digest = channel.createMessage {
            content = buildString {
                append(
                    "# News digest for ${
                        ZonedDateTime.now(ketchupZone).month.name.toLowerCase().capitalize()
                    } ${ZonedDateTime.now(ketchupZone).year}\n\n"
                )
            }
        }
        val thread = channel.startPublicThreadWithMessage(digest.id, "News digest")

        data.forEach {
            thread.createMessage {
                content = buildString {
                    appendLine("## ${it.title}\n")
                    appendLine("${it.description}\n")
                    appendLine("[See more](${it.url})")
                }
            }
        }
    }
}