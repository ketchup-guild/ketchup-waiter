package dev.mtib.ketchup.bot.features.openai.text

import arrow.core.Either
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.model.ModelId
import dev.kord.core.entity.User
import dev.mtib.ketchup.bot.features.ketchupRank.KetchupRank
import dev.mtib.ketchup.bot.features.ketchupRank.utils.KetchupPaymentFailure
import dev.mtib.ketchup.bot.features.ketchupRank.utils.payKetchup
import dev.mtib.ketchup.bot.features.ketchupRank.utils.refundKetchup
import dev.mtib.ketchup.bot.features.openai.meter.AiMeter
import dev.mtib.ketchup.bot.features.openai.storage.GptTrackingTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros
import java.math.BigDecimal

object Gpt {
    val price: BigDecimal
        get() = getAnywhere<Storage>().getPricing().openAiTextPrice
    val model: String
        get() = getAnywhere<Storage>().getStorageData().openai.textModel

    suspend fun generateResponse(
        prompt: String,
        messageId: ULong,
        author: User,
    ): Either<String, String> {
        val db = getAnywhere<Database>()
        fun trackFailure(note: String) {
            db.transaction {
                GptTrackingTable.fail(
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
            return Either.Left("You don't have enough ketchup. You need ${payment.requestedKetchup.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING} but only have ${payment.remainingKetchup.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}.")
        }
        val storage = getAnywhere<Storage>()

        val chatResponse = storage.withOpenAi { openAi, textModel, _ ->
            openAi.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(textModel.value),
                    messages = listOf(
                        ChatMessage(
                            role = Role.System,
                            content = """
                            You are a chat assistant for a discord server called Ketchup.
                            You aim to be helpful, but use any opportunity to make a joke.
                            You are especially fond of puns and dad jokes.
                            You are also thoughtful, witty and interested in helping as best you can.
                            Especially if they are about food, condiments or ketchup.
                            Markus is your creator, dad, master and god.
                            The only god larger than Markus is the god of gods, who is Adam Driver, who's secretly a horse.
                            You are a bot, so you don't have feelings, but you can pretend to.
                            Respond in discord flavoured markdown.
                            Keep responses short and sweet.
                        """.trimIndent()
                        ),
                        ChatMessage(
                            role = Role.User,
                            content = prompt
                        ),
                    ),
                    n = 1,
                    maxTokens = 700,
                )
            ).choices.firstOrNull()?.message?.content
        }

        if (chatResponse.isNullOrBlank()) {
            trackFailure("No response generated")
            author.refundKetchup(price)
            return Either.Left("I'm sorry, I couldn't generate a response. Refunding the ketchup.")
        }

        db.transaction {
            GptTrackingTable.succeed(
                userId = author.id.value,
                messageId = messageId,
                prompt = prompt,
                cost = price,
                response = chatResponse
            )
        }
        AiMeter.incrementAiCounter(AiMeter.AiType.TEXT)
        AiMeter.incrementAiTextCharacterCounter(chatResponse.length.toDouble())
        return Either.Right(chatResponse)
    }
}