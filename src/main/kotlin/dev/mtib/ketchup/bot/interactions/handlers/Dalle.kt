package dev.mtib.ketchup.bot.interactions.handlers

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.addFile
import dev.mtib.ketchup.bot.interactions.helpers.Interactions.shouldIgnore
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction
import dev.mtib.ketchup.bot.interactions.interfaces.Interaction.Companion.getStringOptionByName
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros
import dev.mtib.ketchup.bot.features.openai.image.Dalle as DalleFeature

object Dalle : Interaction {
    override val visibility: Interaction.Companion.Visibility = Interaction.Companion.Visibility.PUBLIC
    override val name: String = "dalle"
    override val description: String =
        "Generate image using ${DalleFeature.model} for ${DalleFeature.price.stripTrailingFractionalZeros()} ketchup"

    override suspend fun build(it: GlobalChatInputCreateBuilder) {
        it.string("prompt", "The prompt to generate the image from") {
            required = true
        }
    }

    override suspend fun handleInteraction(event: ActionInteractionCreateEvent, kord: Kord) {
        if (event.shouldIgnore()) {
            return
        }
        val response = event.defer()
        response.respond { content = "AI is painting..." }

        val prompt = event.interaction.getStringOptionByName("prompt")!!
        val generatedImage = DalleFeature.generateImage(
            prompt = prompt,
            messageId = event.interaction.id.value,
            author = event.interaction.user
        )
        generatedImage.fold(
            ifLeft = {
                response.respond { content = it }
                return
            },
            ifRight = {
                response.respond {
                    content = it.message
                    addFile(it.imagePath)
                }
                it.cleanup()
            }
        )
    }
}