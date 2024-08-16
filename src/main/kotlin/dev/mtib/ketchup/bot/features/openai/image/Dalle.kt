package dev.mtib.ketchup.bot.features.openai.image

import arrow.core.Either
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.model.ModelId
import dev.kord.core.entity.User
import dev.mtib.ketchup.bot.features.ketchupRank.KetchupRank
import dev.mtib.ketchup.bot.features.ketchupRank.utils.KetchupPaymentFailure
import dev.mtib.ketchup.bot.features.ketchupRank.utils.payKetchup
import dev.mtib.ketchup.bot.features.ketchupRank.utils.refundKetchup
import dev.mtib.ketchup.bot.features.openai.storage.DalleTrackingTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.nio.file.Path
import kotlin.io.path.writeBytes

object Dalle {
    val price: BigDecimal
        get() = getAnywhere<Storage>().getPricing().openAiImagePrice
    val model: String
        get() = getAnywhere<Storage>().getStorageData().openai.imageModel

    data class Response(
        val message: String,
        val imageUrl: String,
        val imagePath: Path,
    ) {
        fun cleanup() {
            imagePath.toFile().delete()
        }
    }

    suspend fun generateImage(
        prompt: String,
        messageId: ULong,
        author: User,
    ): Either<String, Response> {
        val db = getAnywhere<Database>()
        fun trackFailure(note: String) {
            db.transaction {
                DalleTrackingTable.fail(
                    userId = author.id.value,
                    messageId = messageId,
                    prompt = prompt,
                    cost = price,
                    note = note
                )
            }
        }
        if (prompt.isBlank()) {
            trackFailure("No prompt provided")
            return Either.Left("Please provide a prompt")
        }
        val payment = author.payKetchup(price)
        if (payment is KetchupPaymentFailure) {
            trackFailure("Not enough ketchup")
            return Either.Left(
                "You don't have enough ketchup. You need ${payment.requestedKetchup.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING} but only have ${payment.remainingKetchup.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}."
            )
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
            author.refundKetchup(price)
            return Either.Left("An error occurred while generating the image. Refunding the ketchup.")
        }

        val response = try {
            val client = OkHttpClient()
            val imagePath = client.newCall(
                Request.Builder().get().url(image.url).build()
            ).execute().use { response ->
                val path = kotlin.io.path.createTempFile(suffix = ".png")
                path.writeBytes(response.body!!.bytes())
                path
            }

            Response(
                message = "Here you go. You have ${payment.asSuccess().remainingKetchup.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING} left.",
                imageUrl = image.url,
                imagePath = imagePath,
            )
        } catch (e: Exception) {
            trackFailure("Error while sending image")
            author.refundKetchup(price)
            return Either.Left("An error occurred while sending the image. Refunding the ketchup.")
        }

        db.transaction {
            DalleTrackingTable.succeed(
                userId = author.id.value,
                messageId = messageId,
                prompt = prompt,
                responseUrl = image.url,
                cost = price
            )
        }
        return Either.Right(response)
    }
}