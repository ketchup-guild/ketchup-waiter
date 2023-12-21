package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.storage.Storage.MagicWord
import dev.mtib.ketchup.bot.utils.getAnywhere
import mu.KotlinLogging
import org.koin.mp.KoinPlatform

class HelpCommand(private val magicWord: MagicWord) : Command(
    "help",
    "Prints help message",
    "Prints help message, can be used with a command name to get more info about a command",
) {
    override val category = Category.Misc
    override val completeness = Completeness.Complete

    private val logger = KotlinLogging.logger { }
    private val commands by lazy { KoinPlatform.getKoin().getAll<Command>() }

    private val helpMessage by lazy {
        buildString {
            appendLine("Help is on the way!")
            appendLine("## Available commands")
            val categoryCommandMap = commands.groupBy { it.category }
            categoryCommandMap.keys.sortedBy { it.name }.forEach { category ->
                appendLine("\n**${category.name}**")
                categoryCommandMap[category]?.sortedBy { it.commandName }?.forEach {
                    appendLine("- ${it.toShortHelpString()}")
                }
            }
            appendLine()
            appendLine("To get more info about a specific command, use `${magicWord} $commandName <command>`")
        }
    }

    companion object {
        fun Command.toShortHelpString(): String {
            val magicWord = getAnywhere<MagicWord>()
            return buildString {
                append("`$magicWord $commandName` - $commandShortDescription")

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
            val magicWord = getAnywhere<MagicWord>()
            return "`$magicWord $commandName`: $commandHelp\n\nState: ${completeness.emoji}"
        }
    }

    override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            if (message.author?.isBot != false) {
                return@on
            }
            if (message.content == "$magicWord help") {
                message.reply {
                    content = helpMessage
                }
            } else if (message.content.startsWith("$magicWord help ")) {
                val commandName = message.content.removePrefix("$magicWord help ").trim()
                val command = commands.find { it.commandName == commandName }
                message.reply {
                    content = command?.toLongHelpString() ?: "Command by that name is not found"
                }
            }
        }
    }
}