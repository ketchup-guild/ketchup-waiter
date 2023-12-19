package dev.mtib.ketchup.bot.storage

import dev.kord.common.entity.Snowflake
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

    companion object {
        const val PATH = "storage.json"
        val Format = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val module = module {
            single { Storage() }
            single { MagicWord(get()) }
            single { Gods(get()) }
            single { Emoji(get()) }
        }
    }

    @Serializable
    data class StorageData(
        val gods: List<String> = listOf("168114573826588681"),
        val magicWord: String = "ketchup",
        val joinEmoji: String = "👀",
    ) {
        val godSnowflakes: List<Snowflake>
            get() = gods.map { Snowflake(it) }
    }

    fun getStorageData(): StorageData {
        return try {
            val data = Path(PATH).readText()
            Format.decodeFromString(data)
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