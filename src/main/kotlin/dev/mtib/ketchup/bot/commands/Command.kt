package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere

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
        Games("Games"),
        Event("Event");
    }

    enum class Completeness(val emoji: String) {
        Complete("✔"),
        WIP("🚧"),
        Stubbed("🧱"),
        Deprecated("🚫");
    }

    open val category = Category.Misc
    open val completeness = Completeness.WIP

    val magicWord by lazy { getAnywhere<Storage.MagicWord>() }
    open val prefix by lazy { "$magicWord $commandName" }
    abstract suspend fun register(kord: Kord)
}