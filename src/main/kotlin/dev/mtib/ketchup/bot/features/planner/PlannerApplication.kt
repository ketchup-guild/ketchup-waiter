package dev.mtib.ketchup.bot.features.planner

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.mtib.ketchup.bot.utils.getEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

val logger = KotlinLogging.logger {}

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    logger.info { "Starting planner" }
    val token = getEnv("KETCHUP_BOT_TOKEN")
    val kord = Kord(token)

    Planner.register(kord)

    kord.on<MessageCreateEvent> {
        if (message.content == "kill planner") {
            kord.shutdown()
        }
    }

    val mutex = Mutex().also { it.lock() }
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            logger.info { "Shutting down kord bot" }
            kord.shutdown()
            mutex.lock()
        }
    })

    logger.info { "Starting kord bot" }
    kord.login {
        intents = Intents {
            +Intent.MessageContent
            +Intent.GuildMessages
            +Intent.GuildMessageReactions
            +Intent.Guilds
            +Intent.DirectMessages
            +Intent.GuildMembers
            +Intent.GuildPresences
        }
    }

    logger.info { "Kord bot shut down" }

    Planner.cancel()

    logger.info { "Done" }
    mutex.unlock()
}