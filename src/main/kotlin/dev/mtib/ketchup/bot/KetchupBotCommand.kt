package dev.mtib.ketchup.bot

import org.koin.core.context.startKoin
import org.koin.dsl.module

private fun getEnv(name: String): String {
    val value = System.getenv(name)
    if (value == null || value.isBlank()) {
        println("$name environment variable not set")
        System.exit(1)
    }
    return value
}

data class KetchupBotToken(val token: String)
data class KetchupBotClientId(val clientId: String)

class BotAuthorizationUrl(id: KetchupBotClientId) {
    val url = "https://discord.com/oauth2/authorize?client_id=${id.clientId}&scope=bot&permissions=${KetchupBot.PERMISSIONS}"
}

suspend fun main() {
    val token = KetchupBotToken(getEnv("KETCHUP_BOT_TOKEN"))
    val clientId = KetchupBotClientId(getEnv("KETCHUP_BOT_CLIENT_ID"))

    val koin = startKoin {
        modules(
            module {
                single { token }
                single { clientId }
                single { KetchupBot(get()) }
                single { BotAuthorizationUrl(get()) }
            },
        )
    }.koin

    println(koin.get<BotAuthorizationUrl>().url)

    koin.get<KetchupBot>().start()
}