package dev.mtib.ketchup.bot.features.aoc

import com.fasterxml.jackson.module.kotlin.readValue
import dev.mtib.ketchup.bot.utils.ketchupObjectMapper
import dev.mtib.ketchup.bot.utils.ketchupZone
import dev.mtib.ketchup.bot.utils.now
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ClientTest : FunSpec({
    val json = """
        {
            "event": "2024",
            "owner_id": 2465123,
            "members": {
                "724629": {
                    "local_score": 0,
                    "stars": 0,
                    "id": 724629,
                    "global_score": 0,
                    "last_star_ts": 0,
                    "name": "LFalch",
                    "completion_day_level": {}
                },
                "2320819": {
                    "completion_day_level": {},
                    "name": "gaetjen",
                    "last_star_ts": 0,
                    "global_score": 0,
                    "id": 2320819,
                    "stars": 0,
                    "local_score": 0
                },
                "2465123": {
                    "global_score": 0,
                    "last_star_ts": 0,
                    "completion_day_level": {},
                    "name": "Markus Becker",
                    "local_score": 0,
                    "stars": 0,
                    "id": 2465123
                }
            }
        }
    """.trimIndent()
    context("deserialisation") {
        test("response") {
            val leaderboard: Client.Leaderboard = ketchupObjectMapper.readValue(json)

            leaderboard.event shouldBe "2024"
            leaderboard.ownerId shouldBe 2465123
            leaderboard.members.size shouldBe 3
            leaderboard.members["724629"]?.name shouldBe "LFalch"
            leaderboard.members["2320819"]?.name shouldBe "gaetjen"
            leaderboard.members["2465123"]?.name shouldBe "Markus Becker"
        }

        test("cache") {
            val leaderboard: Client.Leaderboard = ketchupObjectMapper.readValue(json)
            val cache =
                Client.Cache(listOf(Client.Cache.CacheItem(ketchupZone.now().toInstant(), leaderboard, "cookie")))

            val ser = ketchupObjectMapper.writeValueAsString(cache)
            val deser = ketchupObjectMapper.readValue<Client.Cache>(ser)

            deser.items.size shouldBe 1
        }

    }
})