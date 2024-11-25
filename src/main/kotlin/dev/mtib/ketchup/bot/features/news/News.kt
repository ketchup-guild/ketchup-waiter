package dev.mtib.ketchup.bot.features.news

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.model.ModelId
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.TextChannel
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.bot.features.planner.storage.Locations
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.ketchupZone
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.time.ZonedDateTime
import java.util.*
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

        val openaiSummary = Storage().withOpenAi { openAi, textModel, _ ->
            openAi.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(textModel.value),
                    messages = listOf(
                        ChatMessage.System(
                            """
                            You are a helpful discord AI assistant that helps a group of young adults to organise events.
                            You will receive a list of events and news articles and you will pick a few to outline and prompt the members
                            of the group to pick what they want to do.
                            
                            The members are interested in a variety of activities, such as sports, games, and movies. Lots of them have a technical background.
                            
                            Your response can contain formatted discord markdown, but it should be the body of the message only, as other information will be added to it.
                            The message is also automatically going to get annotated with all the raw data and links, so you don't need to worry about that.
                            
                            The goal is to provide a summary of the news articles and events in a way that is engaging and informative.
                            Filter out any irrelevant information and focus on the most important details.
                            Remove any advertisments only relevant for tourists.
                            Group similar events into categories and provide a brief overview of each category if there are multiple events in the same category.
                            
                            You can also include prompts to do activities or events common to do in the 2 months following ${
                                ZonedDateTime.now(
                                    ketchupZone
                                )
                            }.
                            These other activities could be seasonal things like building gingerbread houses in winter, painting eggs around easter or similar creative activities.
                            If the normal events don't provide enough inspiration, you can also suggest recurring non-seasonal activities like board game nights, D&D one-shot ideas, movie nights, video games or similar.
                        """.trimIndent()
                        ),
                    ) + data.map {
                        ChatMessage.User("${it.title}\n\n${it.description}")
                    },
                )
            ).choices.first().message.content!!
        }

        val digest = channel.createMessage {
            content = buildString {
                append(
                    "# News digest for ${
                        ZonedDateTime.now(ketchupZone).month.name.lowercase(Locale.getDefault())
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    } ${ZonedDateTime.now(ketchupZone).year}\n\n${openaiSummary}"
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