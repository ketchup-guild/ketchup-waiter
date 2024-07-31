package dev.mtib.ketchup.bot.commands

import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.mtib.ketchup.bot.commands.HelpCommand.toLongHelpString
import dev.mtib.ketchup.bot.utils.getCommandArgs

class DiceCommand : ChannelCommand(
    COMMAND,
    "Roll a dice.",
    "Roll a dice: `$magicWord $COMMAND <number of sides>`, e.g. `$magicWord $COMMAND 6`.",
) {
    companion object {
        const val COMMAND = "dice"
    }

    override val category: Category = Category.Games
    override val completeness: Completeness = Completeness.WIP

    override suspend fun MessageCreateEvent.handleMessage(author: User) {
        val args = message.getCommandArgs(this@DiceCommand).mapNotNull { it.toIntOrNull() }
        if (args.isEmpty()) {
            message.reply {
                content = this@DiceCommand.toLongHelpString()
            }
            return
        }
        val rolls = args.map { sides ->
            sides to (1..sides).random()
        }
        val response = buildString {
            appendLine("Rolling dice...")
            appendLine(rolls.joinToString("\n") { "- ${it.first} sided dice: **${it.second}**" })
            appendLine("Total: **${rolls.sumOf { it.second }}**")
            appendLine("Max: ${rolls.maxOf { it.second }}")
            appendLine("Min: ${rolls.minOf { it.second }}")
            appendLine("Average: ${rolls.sumOf { it.second }.toDouble() / rolls.size}")
            appendLine("Theoretical max total: ${rolls.sumOf { it.first }}")
        }
        message.reply {
            content = response
        }
    }
}