package dev.mtib.ketchup.bot.utils

fun <T : Any?> Iterable<T>.joinToAndString(
    separator: String = ", ",
    and: String = " and ",
    map: (T) -> String = { it.toString() }
): String {
    val list = this.asSequence().toList()
    return when {
        list.isEmpty() -> ""
        list.size == 1 -> map(list.first())
        else -> list.dropLast(1).joinToString(separator, transform = map) + and + map(list.last())
    }
}