package dev.mtib.ketchup.bot.features.subscriptions

import arrow.core.Either
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.Role

class Subscriptions {
    companion object {
        /**
         * Regex for role mentions like "<@&1234567890>"
         */
        val roleRegex = Regex("<@&([0-9]+)>")
        suspend fun getRoleFromMention(mention: String, message: Message): Either<String, Role> {
            return getRoleFromMention(mention, message.getGuild())
        }

        suspend fun getRoleFromMention(mention: String, guild: Guild): Either<String, Role> {
            val roleMatch = roleRegex.matchEntire(mention)?.groups?.get(1)
                ?: return Either.Left("Invalid role mention")
            val roleId = Snowflake(roleMatch.value)
            val role = guild.getRole(roleId)
            return Either.Right(role)
        }
    }
}