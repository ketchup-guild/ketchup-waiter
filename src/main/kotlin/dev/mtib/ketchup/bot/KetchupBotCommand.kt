package dev.mtib.ketchup.bot

import dev.mtib.ketchup.bot.commands.Commands
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getEnv
import dev.mtib.ketchup.server.Server
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.util.*

private val logger = KotlinLogging.logger { }

data class KetchupBotToken(val token: String)
data class KetchupBotClientId(val clientId: String)

class BotAuthorizationUrl(id: KetchupBotClientId) {
    val url =
        "https://discord.com/oauth2/authorize?client_id=${id.clientId}&scope=bot&permissions=${KetchupBot.PERMISSIONS}"
}

suspend fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val token = KetchupBotToken(getEnv("KETCHUP_BOT_TOKEN"))
    val clientId = KetchupBotClientId(getEnv("KETCHUP_BOT_CLIENT_ID"))

    val koin = startKoin {
        modules(
            module {
                single { token }
                single { clientId }
                single { KetchupBot(get()) }
                single { BotAuthorizationUrl(get()) }
                single { Database() }
            },
            Commands.module,
            Storage.module,
        )
    }.koin


    logger.info { koin.get<BotAuthorizationUrl>().url }

    withContext(Dispatchers.IO) {
        launch {
            koin.get<KetchupBot>().start()
        }
        launch {
            Server.start()
        }
    }
}