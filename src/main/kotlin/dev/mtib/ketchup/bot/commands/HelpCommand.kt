package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import mu.KotlinLogging
import org.koin.mp.KoinPlatform

object HelpCommand : Command(
    "help",
    "Prints help message",
    "Prints help message, can be used with a command name to get more info about a command",
) {
    override val category = Category.Misc
    override val completeness = Completeness.Complete

    private val logger = KotlinLogging.logger { }
    private val commands by lazy { KoinPlatform.getKoin().getAll<Command>() }

    val globalHelpMessage by lazy {
        buildString {
            appendLine("Help is on the way!")
            appendLine("## Available commands")
            val categoryCommandMap = commands.groupBy { it.category }
            categoryCommandMap.keys.sortedBy { it.name }.forEach { category ->
                appendLine("\n**${category.name}**")
                categoryCommandMap[category]?.sortedBy { it.name }?.forEach {
                    appendLine("- ${it.toShortHelpString()}")
                }
            }
            appendLine()
            appendLine("To get more info about a specific command, use `$magicWord $name <command>`")
        }
    }

    fun Command.toShortHelpString(): String {
        return buildString {
            append("`$magicWord $name` - $commandShortDescription")

            val notes = buildList {
                if (this@toShortHelpString is AdminCommand) {
                    add("admin only")
                }
            }

            if (this@toShortHelpString.completeness != Completeness.Complete) {
                append(" ")
                append(this@toShortHelpString.completeness.emoji)
            }

            if (notes.isNotEmpty()) {
                append(" (")
                append(notes.joinToString(", "))
                append(")")
            }
        }
    }

    fun Command.toLongHelpString(): String {
        return "`$magicWord $name`: $commandHelp\n\nState: ${completeness.emoji}"
    }

    override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            if (message.author?.isBot != false) {
                return@on
            }
            if (message.content == "$magicWord help") {
                message.reply {
                    content = globalHelpMessage
                }
            } else if (message.content.startsWith("$magicWord help ")) {
                val commandName = message.content.removePrefix("$magicWord help ").trim()
                val command = commands.find { it.name == commandName }
                message.reply {
                    content = command?.toLongHelpString() ?: "Command by that name is not found"
                }
            }
        }
    }
}