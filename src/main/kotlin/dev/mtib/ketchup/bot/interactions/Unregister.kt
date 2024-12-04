package dev.mtib.ketchup.bot.interactions

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.exception.EntityNotFoundException
import dev.mtib.ketchup.bot.utils.getEnv
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

suspend fun main(args: Array<String>) = coroutineScope {
    if (args.isEmpty()) {
        println("Please provide the command id(s) as an argument.")
        return@coroutineScope
    }
    val kord = Kord(getEnv("KETCHUP_BOT_TOKEN"))

    args.forEach {
        try {
            withTimeout(3.seconds) {
                val command = kord.getGlobalApplicationCommand(Snowflake(it))
                println("Deleting command ${command.name}")
                command.delete()
            }
        } catch (e: EntityNotFoundException) {
            println("Command $it not found (already deleted?)")
        } catch (e: TimeoutCancellationException) {
            println("Timed out deleting command $it (may already be deleted)")
        }
    }
    println("cleaning up")

    kord.shutdown()

    println("done")

    kord.cancel()
}