package dev.mtib.ketchup.bot.storage

import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import dev.kord.common.entity.Snowflake
import dev.mtib.ketchup.bot.utils.getAnywhere
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class Storage {
    private val logger = KotlinLogging.logger { }

    data class MagicWord(private val storage: Storage) {
        private val value: String
            get() = storage.getStorageData().magicWord

        override fun toString(): String {
            return value
        }
    }

    @Serializable
    data class OpenAiData(
        val apiKey: String,
        val textModel: String,
        val textPrice: String,
        val imageModel: String,
        val imagePrice: String,
    )

    @Serializable
    data class NotionData(
        val integrationToken: String,
        val gamesDatabaseId: String,
    )

    data class Gods(private val storage: Storage) {
        private val value: List<Snowflake>
            get() = storage.getStorageData().godSnowflakes

        operator fun contains(id: Snowflake?): Boolean {
            if (id == null) {
                return false
            }
            return id in value
        }

        fun asList(): List<Snowflake> {
            return value
        }
    }

    data class Emoji(private val storage: Storage) {
        val join: String
            get() = storage.getStorageData().joinEmoji

    }

    data class Flags(private val storage: Storage) {
        val claimPresence: Boolean
            get() = storage.getStorageData().claimPresence

        override fun toString(): String {
            return "Flags(" + listOf(
                ::claimPresence,
            ).joinToString(", ") { "${it.name}=${it.get()}" } + ")"
        }
    }

    data class GuildId(private val storage: Storage) {
        val value: Snowflake
            get() = Snowflake(storage.getStorageData().guildId)
    }

    companion object {
        const val PATH = "storage.json"

        @OptIn(ExperimentalSerializationApi::class)
        val Format = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
        }
        val module = module {
            single { Storage() }
            single { MagicWord(get()) }
            single { Gods(get()) }
            single { Emoji(get()) }
            single { Flags(get()) }
            single { GuildId(get()) }
        }

        fun getMagicWord(): MagicWord = getAnywhere<MagicWord>()
    }

    @Serializable
    data class StorageData(
        val gods: List<String> = listOf("168114573826588681"),
        val magicWord: String = "ketchup",
        val guildId: String = "1118847950743928852",
        val joinEmoji: String = "👀",
        val claimPresence: Boolean = true,
        val openai: OpenAiData = OpenAiData(
            apiKey = "",
            textModel = "",
            textPrice = "0.01",
            imageModel = "",
            imagePrice = "0.5",
        ),
        val notion: NotionData = NotionData(
            integrationToken = "",
            gamesDatabaseId = "",
        ),
    ) {
        val godSnowflakes: List<Snowflake>
            get() = gods.map { Snowflake(it) }
    }

    fun getStorageData(): StorageData {
        return try {
            val data = Path(PATH).readText()
            Format.decodeFromString<StorageData>(data)
        } catch (e: Exception) {
            logger.warn { "Failed to read storage data: $e" }
            val data = StorageData()
            saveStorageData(data)
            data
        }
    }

    fun saveStorageData(data: StorageData) {
        try {
            val json = Format.encodeToString(data)
            Path(PATH).writeText(json)
        } catch (e: Exception) {
            logger.warn { "Failed to save storage data: $e" }
        }
    }

    @JvmInline
    value class TextModel(val value: String)

    @JvmInline
    value class ImageModel(val value: String)

    inline fun <T> withOpenAi(block: (openAi: OpenAI, textModel: TextModel, imageModel: ImageModel) -> T): T {
        val openAiData = getStorageData().openai
        return OpenAI(
            config = OpenAIConfig(
                token = openAiData.apiKey,
                logging = LoggingConfig(logLevel = LogLevel.None),
                /*
                                engine = OkHttpEngine(OkHttpConfig().apply {
                                    ContentNegotiation.let {
                                        println(it)
                                    }
                                    preconfigured = OkHttpClient().newBuilder().build()
                                }),
                */
            )
        ).use { openAi ->
            block(openAi, TextModel(openAiData.textModel), ImageModel(openAiData.imageModel))
        }
    }

    data class Pricing(
        val openAiTextPrice: BigDecimal,
        val openAiImagePrice: BigDecimal,
    )

    fun getPricing(): Pricing {
        val data = getStorageData().openai
        return Pricing(
            openAiTextPrice = data.textPrice.toBigDecimal().setScale(4, RoundingMode.CEILING),
            openAiImagePrice = data.imagePrice.toBigDecimal().setScale(4, RoundingMode.CEILING),
        )
    }
}