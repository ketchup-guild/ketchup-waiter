package dev.mtib.ketchup.bot.commands

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.model.ModelId
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCommandBody

class GptCommand : ChannelCommand(
    commandName = "gpt",
    commandShortDescription = "Generate text using ChatGPT",
    commandHelp = "Generate text using ChatGPT",
) {
    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val body = this.message.getCommandBody(this@GptCommand)
        if (body.isBlank()) {
            message.reply { content = "Please provide a prompt" }
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
            ).choices.firstOrNull()?.message?.content ?: "Oops. No response."
        }

        message.reply {
            content = chatResponse
        }
    }

    override val category: Category
        get() = Category.Misc
    override val completeness: Completeness
        get() = Completeness.Complete

}