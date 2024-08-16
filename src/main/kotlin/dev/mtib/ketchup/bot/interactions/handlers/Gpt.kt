package dev.mtib.ketchup.bot.interactions.handlers

import arrow.core.merge
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.mtib.ketchup.bot.interactions.helpers.Interactions.shouldIgnore
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros
import dev.mtib.ketchup.bot.features.openai.text.Gpt as GptFeature

object Gpt : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PUBLIC
    override val name: String = "gpt"
    override val description: String =
        "Generate text using ${GptFeature.model} for ${GptFeature.price.stripTrailingFractionalZeros()} ketchup"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.string("prompt", "The prompt to generate text from") {
            required = true
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        if (event.shouldIgnore()) {
            return
        }
        val response = event.defer()
        response.respond { content = "AI is thinking..." }

        val prompt = event.interaction.getStringOptionByName("prompt")!!
        val generatedText = GptFeature.generateResponse(
            prompt = prompt,
            messageId = event.interaction.id.value,
            author = event.interaction.user
        )
        response.respond { content = generatedText.merge() }
    }
}