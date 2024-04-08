package dev.mtib.ketchup.bot.commands

import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.model.ModelId
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.addFile
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.getCommandBody
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.io.path.writeBytes

class DalleCommand : ChannelCommand(
    commandName = "dalle",
    commandShortDescription = "Dalle",
    commandHelp = "Dalle",
) {
    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val prompt = this.message.getCommandBody(this@DalleCommand)
        if (prompt.isBlank()) {
            message.reply {
                content = "Please provide a prompt"
            }
            return
        }
        val storage = getAnywhere<Storage>()
        val image = storage.withOpenAi { openAi, _, imageModel ->
            openAi.imageURL(
                ImageCreation(
                    prompt = prompt,
                    n = 1,
                    model = ModelId(imageModel.value)
                )
            ).first()
        }
        val client = OkHttpClient()
        val imagePath = client.newCall(
            Request.Builder().get().url(image.url).build()
        ).execute().use { response ->
            val path = kotlin.io.path.createTempFile(suffix = ".png")
            path.writeBytes(response.body!!.bytes())
            path
        }

        message.reply {
            content = "Here you go"
            addFile(imagePath)
        }

        imagePath.toFile().delete()
    }

    override val category: Category
        get() = Category.Misc
    override val completeness: Completeness
        get() = Completeness.Complete
}