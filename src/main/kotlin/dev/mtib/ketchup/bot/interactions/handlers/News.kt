package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.mtib.ketchup.bot.features.planner.storage.Locations
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.utils.toMessageFormat
import dev.mtib.ketchup.bot.features.news.News as NewsFeature

object News : Interaction {
    override val visibility: Interaction.Companion.Visibility
        get() = Interaction.Companion.Visibility.PUBLIC
    override val name: String
        get() = "news"
    override val description: String
        get() = "Manually parses the news feed and generates a digest for the channel"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        super.build(it)
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        val response = event.defer()

        response.respond {
            content = "Generating news digest..."
        }

        NewsFeature.run(event.interaction.channel.asChannelOf<TextChannel>())

        val ideaChannel = kord.getChannelOf<TextChannel>(Locations.fromEnvironment().ideaChannelSnowflake)

        response.respond {
            content =
                "Manually triggered news digest generated. The next automated news digest will be posted in ${ideaChannel?.mention} ${
                    NewsFeature.getNextExecution().toInstant().toMessageFormat()
                }"
        }
    }
}