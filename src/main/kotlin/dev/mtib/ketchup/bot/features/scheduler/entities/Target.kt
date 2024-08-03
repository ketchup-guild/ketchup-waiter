package dev.mtib.ketchup.bot.features.scheduler.entities

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.channel.TextChannel

sealed class Target {
    data class User(val user: dev.kord.core.entity.User) : Target() {
        override val mention: String
            get() = user.mention

        override suspend fun send(content: String) {
            user.getDmChannel().createMessage(content)
        }
    }

    data class Channel(val channel: dev.kord.core.entity.channel.Channel) : Target() {
        override val mention: String
            get() = channel.mention

        override suspend fun send(content: String) {
            channel.asChannelOf<TextChannel>().createMessage(content)
        }
    }

    companion object {
        suspend fun fromULong(kord: Kord, id: ULong): Target {
            val user = kord.getUser(Snowflake(id))
            if (user != null) {
                return User(user)
            }
            val channel = kord.getChannel(Snowflake(id))
            if (channel != null) {
                return Channel(channel)
            }
            throw IllegalArgumentException("No user or channel found with id $id")
        }
    }

    abstract val mention: String
    abstract suspend fun send(content: String)
}