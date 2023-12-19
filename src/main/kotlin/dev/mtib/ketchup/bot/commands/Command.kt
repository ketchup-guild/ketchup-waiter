package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord

abstract class Command(
    val commandName: String,
    val commandShortDescription: String,
    val commandHelp: String,
) {
    enum class Category(val prettyName: String) {
        Misc("Miscellaneous"),
        Admin("Admin"),
        Club("Club"),
        Role("Role"),
        Event("Event");
    }

    open val category = Category.Misc
    abstract suspend fun register(kord: Kord)
}