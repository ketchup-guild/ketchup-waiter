package dev.mtib.ketchup.bot.features.aoc

import com.fasterxml.jackson.databind.JsonNode
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel
import dev.mtib.ketchup.bot.utils.getEnv
import dev.mtib.ketchup.bot.utils.ketchupObjectMapper

suspend fun main() {
    val year = System.getenv("AOC_YEAR")!!.toInt()
    val ownerId = System.getenv("AOC_OWNER_ID")!!.toLong()
    val cookie = System.getenv("AOC_COOKIE")!!

    val item = Client.getLeaderboard(year, ownerId, cookie)

    println(ketchupObjectMapper.convertValue(item, JsonNode::class.java).toPrettyString())

    val channel = System.getenv("AOC_CHANNEL")

    channel?.let { channelId ->
        val kord = Kord(getEnv("KETCHUP_BOT_TOKEN"))

        val channel = kord.getChannelOf<TextChannel>(Snowflake(channelId))!!

        AocPoster.post(channel, item)

        kord.shutdown()
    }
}
