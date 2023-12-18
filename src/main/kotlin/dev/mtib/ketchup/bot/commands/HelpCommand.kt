package dev.mtib.ketchup.bot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.mtib.ketchup.bot.KetchupBot.Companion.MAGIC_WORD
import mu.KotlinLogging
import org.koin.mp.KoinPlatform

class HelpCommand : Command(
    "help",
    "Prints help message",
    "Prints help message, can be used with a command name to get more info about a command",
) {
    private val logger = KotlinLogging.logger { }
    private val commands by lazy { KoinPlatform.getKoin().getAll<Command>() }

    private val helpMessage by lazy {
        buildString {
            appendLine("Help is on the way!\n")
            appendLine("To get more info about a specific command, use `${MAGIC_WORD} $commandName <command>`\n")
            appendLine("**Available commands:**")
            commands.forEach {
                append("- `${MAGIC_WORD} ${it.commandName}` - ${it.commandShortDescription}")
                if (it is AdminCommand) {
                    append(" (admin only)")
                }
                appendLine()
            }
        }
    }

    override suspend fun register(kord: Kord) {
        kord.on<MessageCreateEvent> {
            if (message.author?.isBot != false) {
                return@on
            }
            if (message.content == "$MAGIC_WORD help") {
                message.reply {
                    content = helpMessage
                }
            } else if (message.content.startsWith("$MAGIC_WORD help ")) {
                val commandName = message.content.removePrefix("$MAGIC_WORD help ").trim()
                val command = commands.find { it.commandName == commandName }
                if (command != null) {
                    message.reply {
                        content = "`$MAGIC_WORD ${command.commandName}`: ${command.commandHelp}"
                    }
                } else {
                    message.reply {
                        content = "Command by that name is not found"
                    }
                }
            }
        }
    }
}