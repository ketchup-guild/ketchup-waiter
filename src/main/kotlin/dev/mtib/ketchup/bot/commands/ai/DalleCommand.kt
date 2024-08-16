package dev.mtib.ketchup.bot.commands.ai

import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.addFile
import dev.mtib.ketchup.bot.commands.ChannelCommand
import dev.mtib.ketchup.bot.features.ketchupRank.KetchupRank
import dev.mtib.ketchup.bot.features.openai.image.Dalle
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCommandBody
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros

class DalleCommand : ChannelCommand(
    commandName = "dalle",
    commandShortDescription = "Generate an image from a prompt using DALL-E (${getAnywhere<Storage>().getPricing().openAiImagePrice.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}/image)",
    commandHelp = "Generate an image from a prompt using DALL-E model \"${getAnywhere<Storage>().getStorageData().openai.imageModel}\". Cost ${getAnywhere<Storage>().getPricing().openAiImagePrice.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}/image.",
) {
    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        Dalle.generateImage(
            prompt = this.message.getCommandBody(this@DalleCommand),
            messageId = this.message.id.value,
            author = author,
        ).fold(
            ifLeft = {
                this.message.reply {
                    content = it
                }
            },
            ifRight = {
                this.message.reply {
                    content = it.message
                    addFile(it.imagePath)
                }
                it.cleanup()
            }
        )
    }

    override val category: Category
        get() = Category.Misc
    override val completeness: Completeness
        get() = Completeness.Complete
}