package dev.mtib.ketchup.bot.commands

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.model.ModelId
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.features.ketchupRank.KetchupRank
import dev.mtib.ketchup.bot.features.ketchupRank.utils.KetchupPaymentFailure
import dev.mtib.ketchup.bot.features.ketchupRank.utils.payKetchup
import dev.mtib.ketchup.bot.features.ketchupRank.utils.refundKetchup
import dev.mtib.ketchup.bot.features.openai.storage.GptTrackingTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCommandBody
import dev.mtib.ketchup.bot.utils.stripTrailingFractionalZeros

class GptCommand : ChannelCommand(
    commandName = "gpt",
    commandShortDescription = "Generate text using ChatGPT (${getAnywhere<Storage>().getPricing().openAiTextPrice.stripTrailingFractionalZeros()}${KetchupRank.KETCHUP_EMOJI_STRING}/prompt)",
    commandHelp = "Generate text using ChatGPT using the model \"${getAnywhere<Storage>().getStorageData().openai.textModel}. Usage: `!gpt <prompt>`. Cost: ${getAnywhere<Storage>().getPricing().openAiTextPrice.stripTrailingFractionalZeros()} ${KetchupRank.KETCHUP_EMOJI_STRING}",
) {
    private val price by lazy { getAnywhere<Storage>().getPricing().openAiTextPrice }

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val body = this.message.getCommandBody(this@GptCommand)
        val db = getAnywhere<Database>()
        fun trackFailure(note: String) {
            db.transaction {
                GptTrackingTable.fail(
                    userId = author.id.value,
                    messageId = message.id.value,
                    prompt = body,
                    cost = price,
                    note = note
                )
            }
        }
        if (body.isBlank()) {
            trackFailure("No prompt provided")
            message.reply { content = "Please provide a prompt" }
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
                            content = body
                        ),
                    ),
                    n = 1,
                )
            ).choices.firstOrNull()?.message?.content
        }

        if (chatResponse.isNullOrBlank()) {
            trackFailure("No response generated")
            message.reply {
                content = "I'm sorry, I couldn't generate a response. Refunding the ketchup."
            }
            author.refundKetchup(price)
            return
        }

        message.reply {
            content = chatResponse
        }
        db.transaction {
            GptTrackingTable.succeed(
                userId = author.id.value,
                messageId = message.id.value,
                prompt = body,
                cost = price,
                response = chatResponse
            )
        }
    }

    override val category: Category
        get() = Category.Misc
    override val completeness: Completeness
        get() = Completeness.Complete

}