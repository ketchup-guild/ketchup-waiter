package dev.mtib.ketchup.bot.commands.ai

import arrow.core.merge
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.commands.ChannelCommand
import dev.mtib.ketchup.bot.features.ketchupRank.KetchupRank
import dev.mtib.ketchup.bot.features.openai.text.Gpt
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCommandBody
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros

class GptCommand : ChannelCommand(
    commandName = "gpt",
    commandShortDescription = "Generate text using ChatGPT (${getAnywhere<Storage>().getPricing().openAiTextPrice.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}/prompt)",
    commandHelp = "Generate text using ChatGPT using the model \"${getAnywhere<Storage>().getStorageData().openai.textModel}\". Cost: ${getAnywhere<Storage>().getPricing().openAiTextPrice.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}/prompt",
) {
    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        Gpt.generateResponse(
            prompt = this.message.getCommandBody(this@GptCommand),
            messageId = this.message.id.value,
            author = author,
        ).merge().let {
            this.message.reply {
                content = it
            }
        }
    }

    override val category: Category
        get() = Category.Misc
    override val completeness: Completeness
        get() = Completeness.Complete

}