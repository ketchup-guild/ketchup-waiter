package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord

abstract class Command(
    val commandName: String,
    val commandShortDescription: String,
    val commandHelp: String,
    ) {
    abstract suspend fun register(kord: Kord)
}