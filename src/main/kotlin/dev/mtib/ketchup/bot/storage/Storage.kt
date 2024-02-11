package dev.mtib.ketchup.bot.storage

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.dsl.module
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
    }

    @Serializable
    data class StorageData(
        val gods: List<String> = listOf("168114573826588681"),
        val magicWord: String = "ketchup",
        val guildId: String = "1118847950743928852",
        val joinEmoji: String = "👀",
        val claimPresence: Boolean = true,
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
}