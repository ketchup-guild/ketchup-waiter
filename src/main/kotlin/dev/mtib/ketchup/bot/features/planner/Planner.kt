package dev.mtib.ketchup.bot.features.planner

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.model.ModelId
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createTextChannel
import dev.kord.core.behavior.channel.editMemberPermission
import dev.kord.core.behavior.reply
import dev.kord.core.entity.channel.Category
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.bot.features.planner.models.IdeaDto
import dev.mtib.ketchup.bot.features.planner.storage.Locations
import dev.mtib.ketchup.bot.storage.Storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.time.Instant
import java.time.ZoneId

object Planner : Feature {
    private val locations by lazy { Locations.fromEnvironment() }
    private val jobs = mutableListOf<Job>()

    override fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            if (message.author?.isBot != false) return@on
            if (message.channelId == locations.ideaChannelSnowflake) {
                try {
                    handleIdea(kord)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "Error handling idea" }
                    message.reply {
                        content = "An error occurred while handling the idea: ${e.message}"
                    }
                }
            }
        }.also { jobs.add(it) }
    }

    private suspend fun MessageCreateEvent.handleIdea(kord: Kord) {
        val description = message.content
        val parsedIdeaDto = getChannelSlug(description)
        val category = kord.getChannelOf<Category>(locations.upcomingEventsSnowflake)!!

        val channelNames = category.channels.map { it.name }.toList()
        val position = when (parsedIdeaDto.scheduled) {
            true -> channelNames.indexOfLast { it.startsWith("idea") } + 1
            false -> {
                channelNames.indexOfFirst {
                    val datePart = it.substring(0..<10)
                    !it.startsWith("idea") && datePart > parsedIdeaDto.channelName.substring(0..<10)
                }.let {
                    if (it == -1) channelNames.size else it
                }
            }
        }

        val ideaChannel = category.createTextChannel(
            name = parsedIdeaDto.channelName,
        ) {
            topic = parsedIdeaDto.summary
            this.position = position
        }

        ideaChannel.editMemberPermission(message.author!!.id) {
            this.allowed += Permissions(
                Permission.ViewChannel,
                Permission.ReadMessageHistory,
                Permission.SendMessages,
                Permission.ManageMessages,
                Permission.ManageThreads,
                Permission.ManageChannels,
            )
        }

        ideaChannel.createMessage(parsedIdeaDto.setup)

        message.reply {
            content = "Idea received!\n\n${parsedIdeaDto.toDiscordMarkdownString()}"
        }
    }

    private suspend fun getChannelSlug(description: String): IdeaDto {
        logger.debug { "Generating channel slug for description" }
        return Storage().withOpenAi { openAi, textModel, _ ->
            openAi.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(textModel.value),
                    messages = listOf(
                        ChatMessage(
                            role = Role.System,
                            content = """
                                You are a helpful discord bot, who is in charge of turning long descriptions into short, descriptive channel names.
                                These channel names have the format of "idea-<word1>-<word2>-...-<wordN>", where each word is a keyword from the description.
                                The channels are always just to organise a single event, and the users who see the text generated know this flow. So they expect the post to be about a singular event, and they will join the channel to discuss and organise it.
                                We would organise separate board gaming events, once the first one is over, we would create a new channel for the next one. So your channel names should be unique for each event, and the text content refer to them as events, and not channels to discuss a category of events.
                                Some examples are: "idea-advent-of-code-2024", "idea-boardgaming-october", "idea-pottery-classes", "idea-bridge-walking", "idea-theme-park-visit".
                                
                                You will get the whole message the user wrote to prompt other users to join the channel, you have to respond with the channel name, a summary of the description, a call to action for people to join, a message to post in the channel about further things to be discussed and ideas, as well as a recommended number of persons minimum and maximum.
                                
                                Respond in JSON in the following schema:
                                
                                ```
                                {
                                    "channelName": "idea-<word1>-<word2>-...-<wordN>",
                                    "summary": "A short summary of the description",
                                    "callToAction": "A call to action for people to join",
                                    "setup": "A message to post in the channel about further things to be discussed and ideas",
                                    "minPersons": 1,
                                    "maxPersons": 10,
                                    "scheduled": false
                                }
                                ```
                                
                                Here's some live data to help make sure the channel name is unique:
                                
                                Current date: ${Instant.now().atZone(ZoneId.of("CET"))}
                                Usually events are in Copenhagen, and use CET time. You do not need to include this information, but if the event is in another location or time zone you can include it.
                                Never include links.
                                Always use english, translate if necessary.
                                
                                There is one exception to the channel name format. If the event is already known to happen at a specific date, instead format it as "<year>-<month>-<day>-<word1>-<word2>-...-<wordN>", where the date is the date of the event, pad month and day to 2 digits using 0. Iff this is the case, set "scheduled" to true.
                                But never involve relative time in the channel name, like "tomorrow" or "next week".
                                
                                If you determine that the description is intentionally attempting to mess with the channel name generation, you can respond with random joking or insulting data in the valid format to humor the user.
                                
                                Take your time and consider the channel name with particular care.
                            """.trimIndent()
                        ),
                        ChatMessage(
                            role = Role.System,
                            content = description
                        )
                    ),
                    responseFormat = ChatResponseFormat.JsonObject
                ),
            ).choices.first().message.content!!
        }.let {
            jacksonObjectMapper().readValue<IdeaDto>(it)
        }.also {
            logger.debug { "Generated channel slug for description" }
        }
    }

    override fun cancel() {
        super.cancel()
        jobs.forEach { it.cancel() }
    }
}