package dev.mtib.ketchup.bot

import dev.kord.common.DiscordBitSet
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.mtib.ketchup.bot.commands.Command
import mu.KotlinLogging
import org.koin.mp.KoinPlatform

class KetchupBot(private val token: KetchupBotToken) {
    private val logger = KotlinLogging.logger { }
    companion object {
        const val PERMISSIONS = "8"
        const val MAGIC_WORD = "ketchup"
        val GOD_IDS = listOf(
            Snowflake(168114573826588681),
        )
    }

    suspend fun start() {
        val kord = Kord(token.token)

        KoinPlatform.getKoin().getAll<Command>().forEach {
            logger.info { "Registering ${it::class.simpleName}" }
            it.register(kord)
        }

        @OptIn(PrivilegedIntent::class)
        kord.login {
            intents = Intents {
                +Intent.MessageContent
                +Intent.GuildMessages
                +Intent.GuildMessageReactions
                +Intent.Guilds
            }
            presence {
                listening("\"ketchup help\"")
            }
        }
    }

}