package dev.mtib.ketchup.bot.utils


fun getEnv(name: String): String {
    val value = System.getenv(name)
    if (value == null || value.isBlank()) {
        throw IllegalStateException("$name environment variable not set")
    }
    return value
}