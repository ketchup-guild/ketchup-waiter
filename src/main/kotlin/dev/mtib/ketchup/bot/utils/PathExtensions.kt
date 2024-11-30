package dev.mtib.ketchup.bot.utils

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path


inline fun <reified T> Path.read(): T {
    return ketchupObjectMapper.readValue(this.toFile())
}

inline fun <reified T> Path.readOrNull(): T? {
    val logger = KotlinLogging.logger {}

    if (!this.toFile().exists()) return null
    return try {
        ketchupObjectMapper.readValue(this.toFile())
    } catch (e: Exception) {
        logger.warn { "Failed to read file $this: $e" }
        null
    }
}

inline fun <reified T> Path.write(value: T) {
    ketchupObjectMapper.writerWithDefaultPrettyPrinter().writeValue(this.toFile(), value)
}
