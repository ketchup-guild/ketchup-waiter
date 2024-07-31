package dev.mtib.ketchup.bot.errors

import dev.kord.core.entity.User
import dev.mtib.ketchup.bot.commands.Command

class UnauthorizedException(val user: User?, val attemptedCommand: Command) : Exception(
    "User ${user?.username ?: "unknown"} is not authorized to use the command ${attemptedCommand.name}"
)