package dev.mtib.ketchup.bot

import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.mtib.ketchup.bot.commands.Command
import dev.mtib.ketchup.bot.storage.Storage.MagicWord
import dev.mtib.ketchup.bot.utils.getAllAnywhere
import dev.mtib.ketchup.bot.utils.getAnywhere
import mu.KotlinLogging

class KetchupBot(private val token: KetchupBotToken) {
    private val logger = KotlinLogging.logger { }

    companion object {
        const val PERMISSIONS = "8"
    }

    suspend fun start() {
        val kord = Kord(token.token)

        val magicWord = getAnywhere<MagicWord>()
        getAllAnywhere<Command>().forEach {
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
                +Intent.DirectMessages
            }
            presence {
                listening("\"$magicWord help\"")
            }
        }
    }

}