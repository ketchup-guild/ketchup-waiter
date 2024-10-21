package dev.mtib.ketchup.bot.utils

fun <T> T?.orThrow(message: String): T {
    return this ?: throw IllegalStateException(message)
}

fun <T> T?.orThrow(message: () -> String): T {
    return this.orThrow(message())
}