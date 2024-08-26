package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.mtib.ketchup.bot.interactions.handlers.ToggleRespondToGod
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.isGod
import dev.mtib.ketchup.bot.utils.matchesSignature

abstract class Command(
    val name: String,
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

    companion object {
        val magicWord by lazy { getAnywhere<Storage.MagicWord>() }
    }

    open val prefix by lazy { "$magicWord $name" }
    abstract suspend fun register(kord: Kord)

    open suspend fun matchesSignature(kord: Kord, message: Message): Boolean {
        return message.matchesSignature(this)
    }

    fun User.isIgnoredGod(): Boolean {
        return this.isGod && !ToggleRespondToGod.respondToGod
    }
}