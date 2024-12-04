package dev.mtib.ketchup.bot.features.aoc

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.model.ModelId
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.addFile
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.bot.interactions.handlers.ReportBenchmark
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.ketchupZone
import dev.mtib.ketchup.bot.utils.launchAtClockTime
import dev.mtib.ketchup.bot.utils.now
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.label.labs
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.themes.*
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.deleteExisting
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object AocPoster : Feature {
    lateinit var kord: Kord
    lateinit var job: Job
    private val logger = KotlinLogging.logger {}

    override fun cancel() {
        super.cancel()
        job.cancel()
    }

    override fun register(kord: Kord) {
        this.kord = kord

        job = CoroutineScope(Dispatchers.Default).launch {
            launchAtClockTime(ketchupZone, 20) {
                handlePostListeners()
            }
            launchAtClockTime(ketchupZone, 5, 50) {
                handleThreadListeners()
            }
        }
    }

    suspend fun handlePostListeners() {
        Client.getListeners().forEach { listener ->
            try {
                val eventData = Client.getLeaderboard(listener.event.toInt(), listener.ownerId, listener.cookie)
                if (!eventData.isCurrent() || eventData.members.isEmpty()) return@forEach
                val channel = kord.getChannel(Snowflake(listener.snowflake))?.asChannelOfOrNull<TextChannel>()
                if (channel != null) {
                    post(channel, eventData)
                } else {
                    Client.removeListener(listener)
                }
            } catch (e: Client.RedirectException) {
                kord.getChannel(Snowflake(listener.snowflake))?.asChannelOfOrNull<TextChannel>()?.createMessage {
                    content =
                        "The cookie for the Advent of Code ${listener.event} leaderboard is invalid. To continue receiving updates, set up the AoC cookie again."
                }
                Client.removeListener(listener)
            }
        }
    }

    suspend fun handleThreadListeners() {
        Client.getListeners().forEach { listener ->
            if (listener.event.toInt() != ketchupZone.now().year) return@forEach
            try {
                val channel = kord.getChannelOf<TextChannel>(Snowflake(listener.snowflake)) ?: run {
                    Client.removeListener(listener)
                    return@forEach
                }
                createDailyThread(channel)
            } catch (e: Exception) {
                logger.error(e) { "Failed to create daily thread for listener $listener" }
            }
        }
    }

    suspend fun createDailyThread(channel: TextChannel, day: Int = ketchupZone.now().dayOfMonth) {
        channel.startPublicThread("Day $day") {
            autoArchiveDuration = ArchiveDuration.from(3.days)
        }.let {
            Storage().withOpenAi { openAi, textModel, imageModel ->
                openAi.chatCompletion(
                    ChatCompletionRequest(
                        model = ModelId(textModel.value),
                        messages = listOf(
                            ChatMessage.System(
                                """
                                    You are a discord bot welcoming participants in a daily thread for the Advent of Code.
                                    You are posting a greeting message in the thread at 5:50 AM for the advent of code day $day about to release at 6 AM.
                                    
                                    Motivate members to participate in the daily thread and share their progress.
                                    End the message with either a bit of trivia about the day ($day of December), a fun fact about advent of code or a joke.
                                    
                                    Remind them to use `/${ReportBenchmark.name} <day> <part> <time in ms>` to report their benchmark results.
                                """.trimIndent()
                            ),
                            ChatMessage.User("Generate the daily greeting message for the $day of December thread.")
                        ),
                        n = 1,
                    )
                ).choices.firstOrNull()?.message?.content.let { message ->
                    it.createMessage {
                        content = message ?: "Good morning! ☀️\nHave fun solving day $day."
                    }
                }
            }
        }
    }

    suspend fun post(channel: TextChannel, eventData: Client.Leaderboard, day: Int = ketchupZone.now().dayOfMonth) {
        val benchmarkResults = Client.getBenchmarkResults()[eventData.event]
        val yesterdaysBenchmarkResults = benchmarkResults?.get(day - 1)
        val todaysBenchmarkResults = benchmarkResults?.get(day)

        val userNames = mutableMapOf<String, String>()

        suspend fun Map<String, List<Client.Cache.BenchmarkReport>>.plot(): Path {
            val data = buildMap<String, MutableList<Any>> {
                this@plot.values.flatten().sortedWith(compareBy { it.timestamp }).forEach { data ->
                    try {
                        val user = userNames.getOrPut(data.userSnowflake) {
                            channel.kord.getUser(Snowflake(data.userSnowflake))!!
                                .asMember(channel.guildId).effectiveName
                        }
                        getOrPut("snowflake") { mutableListOf() }.add(data.userSnowflake)
                        getOrPut("username") { mutableListOf() }.add(user)
                        getOrPut("report time") { mutableListOf() }.add(
                            data.timestamp.atZone(ketchupZone).format(
                                java.time.format.DateTimeFormatter.ofPattern("dd. HH:mm")
                            )
                        )
                        getOrPut("runtime") { mutableListOf() }.add(data.timeMs)
                    } catch (e: Exception) {
                        // Skip user
                        logger.error(e) { "Failed to plot benchmark results for ${data.userSnowflake}" }
                    }
                }
            }

            val example = this.values.first().first()
            val id = example.let {
                "${it.event}_${it.day}_${it.part}"
            }

            val darker = "#2f3034"
            val lighter = "#323337"

            return (letsPlot(data) + geomLine {
                x = "report time"
                y = "runtime"
                color = "username"
            } + geomPoint {
                x = "report time"
                y = "runtime"
                color = "username"
            } + ggtitle(
                "Benchmarks ${example.event} - Day ${example.day} - Part ${example.part}",
            ) + labs(
                x = "Report time",
                y = "Runtime [ms]",
                caption = "",
                color = "Elf",
            ) + ggsize(
                width = 800,
                height = 600,
            ) + theme(
                plotBackground = elementRect(fill = lighter),
                title = elementText(color = "#fff"),
                axis = elementLine(color = "#fff"),
                axisText = elementText(color = "#fff"),
                axisTicks = elementLine(color = "#fff"),
                legendBackground = elementBlank(),
                panelGridMajorX = elementLine(color = darker),
                panelGridMajorY = elementLine(color = darker),
                legendText = elementText(color = "#fff"),
            )).let {
                val filename = "benchmark_${id}.png"
                ggsave(it, filename, path = System.getenv("PWD"))
                Path(filename)
            }
        }

        suspend fun Map<String, List<Client.Cache.BenchmarkReport>>.toDiscordMarkdown(): String {
            return buildString {
                this@toDiscordMarkdown.values.sortedBy { it.last().timeMs }.forEach { reports ->
                    val report = reports.last()
                    val user = userNames.getOrPut(report.userSnowflake) {
                        channel.kord.getUser(Snowflake(report.userSnowflake))!!.asMember(channel.guildId).effectiveName
                    }
                    appendLine("- ${user}: ${report.timeMs.milliseconds}")
                }
            }
        }

        channel.createMessage {
            content = buildString {
                appendLine("# Advent of Code ${eventData.event} leaderboard\n")
                if (day <= 25) {
                    appendLine("## Day $day")
                }
                when (day) {
                    6 -> appendLine("Happy Saint Nikolaus'!")
                    24 -> appendLine("Merry Christmas eve!")
                    25 -> appendLine("Merry Christmas day!")
                    26 -> appendLine("Thank you all for participating in this year's Advent of Code! 🎉 Here's the last report.")
                }

                appendLine("\nhttps://adventofcode.com/${eventData.event}/leaderboard/private/view/${eventData.ownerId}")

                val leaderboard =
                    eventData.members.values.sortedWith(compareBy({ -it.stars }, { it.lastStarTs }))

                leaderboard.forEachIndexed { index, member ->
                    appendLine("${index + 1}. ${member.name} - ${member.stars} stars")
                }

                appendLine("Report your benchmark results with `/${ReportBenchmark.name} <day> <part> <time in ms>` (averaging many runs, reading input from memory & JIT warmup allowed)!")

                if (yesterdaysBenchmarkResults?.get(1) != null || yesterdaysBenchmarkResults?.get(2) != null) {
                    appendLine("## Benchmark results from yesterday (Day ${day - 1})")

                    yesterdaysBenchmarkResults[1]?.let {
                        appendLine("### Part 1")
                        append(it.toDiscordMarkdown())
                    }

                    yesterdaysBenchmarkResults[2]?.let {
                        appendLine("### Part 2")
                        append(it.toDiscordMarkdown())
                    }
                }

                if (todaysBenchmarkResults?.get(1) != null || todaysBenchmarkResults?.get(2) != null) {
                    appendLine("## Benchmark results from today (Day $day)")

                    todaysBenchmarkResults[1]?.let {
                        appendLine("### Part 1")
                        append(it.toDiscordMarkdown())
                    }

                    todaysBenchmarkResults[2]?.let {
                        appendLine("### Part 2")
                        append(it.toDiscordMarkdown())
                    }
                }

                if (day != 26) {
                    appendLine("See you tomorrow! 👋")
                }
            }
            listOfNotNull(
                yesterdaysBenchmarkResults?.get(1)?.plot(),
                yesterdaysBenchmarkResults?.get(2)?.plot(),
                todaysBenchmarkResults?.get(1)?.plot(),
                todaysBenchmarkResults?.get(2)?.plot(),
            ).also { delay(500.milliseconds) }.forEach {
                addFile(it)
                CoroutineScope(Dispatchers.Default).launch {
                    delay(20.seconds)
                    it.deleteExisting()
                }
            }
        }
    }
}