package dev.mtib.ketchup.bot.commands

import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.model.ModelId
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.addFile
import dev.mtib.ketchup.bot.features.ketchupRank.KetchupRank
import dev.mtib.ketchup.bot.features.ketchupRank.utils.KetchupPaymentFailure
import dev.mtib.ketchup.bot.features.ketchupRank.utils.payKetchup
import dev.mtib.ketchup.bot.features.ketchupRank.utils.refundKetchup
import dev.mtib.ketchup.bot.features.openai.storage.DalleTrackingTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCommandBody
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.io.path.writeBytes

class DalleCommand : ChannelCommand(
    commandName = "dalle",
    commandShortDescription = "Generate an image from a prompt using DALL-E (${getAnywhere<Storage>().getPricing().openAiImagePrice.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}/image)",
    commandHelp = "Generate an image from a prompt using DALL-E model \"${getAnywhere<Storage>().getStorageData().openai.imageModel}\". Cost ${getAnywhere<Storage>().getPricing().openAiImagePrice.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}/image.",
) {
    private val price by lazy { getAnywhere<Storage>().getPricing().openAiImagePrice }

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val prompt = this.message.getCommandBody(this@DalleCommand)
        val db = getAnywhere<Database>()
        fun trackFailure(note: String) {
            db.transaction {
                DalleTrackingTable.fail(
                    userId = author.id.value,
                    messageId = message.id.value,
                    prompt = prompt,
                    cost = price,
                    note = note
                )
            }
        }
        if (prompt.isBlank()) {
            trackFailure("No prompt provided")
            message.reply {
                content = "Please provide a prompt"
            }
            return
        }
        val payment = author.payKetchup(price)
        if (payment is KetchupPaymentFailure) {
            trackFailure("Not enough ketchup")
            message.reply {
                content =
                    "You don't have enough ketchup. You need ${payment.requestedKetchup.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING} but only have ${payment.remainingKetchup.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}."
            }
            return
        }
        val storage = getAnywhere<Storage>()
        val image = try {
            storage.withOpenAi { openAi, _, imageModel ->
                openAi.imageURL(
                    ImageCreation(
                        prompt = "$prompt and there is a bottle of ketchup hidden in the image",
                        n = 1,
                        model = ModelId(imageModel.value)
                    )
                ).first()
            }
        } catch (e: Exception) {
            trackFailure("Error while generating image")
            message.reply {
                content = "An error occurred while generating the image. Refunding the ketchup."
            }
            author.refundKetchup(price)
            return
        }

        try {
            val client = OkHttpClient()
            val imagePath = client.newCall(
                Request.Builder().get().url(image.url).build()
            ).execute().use { response ->
                val path = kotlin.io.path.createTempFile(suffix = ".png")
                path.writeBytes(response.body!!.bytes())
                path
            }

            message.reply {
                content =
                    "Here you go. You have ${payment.asSuccess().remainingKetchup.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING} left."
                addFile(imagePath)
            }

            imagePath.toFile().delete()
        } catch (e: Exception) {
            trackFailure("Error while sending image")
            message.reply {
                content = "An error occurred while sending the image. Refunding the ketchup."
            }
            author.refundKetchup(price)
            return
        }

        db.transaction {
            DalleTrackingTable.succeed(
                userId = author.id.value,
                messageId = message.id.value,
                prompt = prompt,
                responseUrl = image.url,
                cost = price
            )
        }
    }

    override val category: Category
        get() = Category.Misc
    override val completeness: Completeness
        get() = Completeness.Complete
}