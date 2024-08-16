package dev.mtib.ketchup.bot.features.notion

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.mtib.ketchup.bot.features.notion.models.NotionGame
import dev.mtib.ketchup.bot.storage.Storage
import dev.mtib.ketchup.bot.utils.getAnywhere
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

object NotionClient {
    val client by lazy { OkHttpClient() }
    val data by lazy {
        getAnywhere<Storage>().getStorageData().notion
    }
    val mapper by lazy { jacksonObjectMapper() }
    val logger = KotlinLogging.logger { }

    private fun query(body: Map<Any, Any?> = emptyMap()): Response {
        val startRequest = Request.Builder()
            .url("https://api.notion.com/v1/databases/${data.gamesDatabaseId}/query")
            .header("Authorization", "Bearer ${data.integrationToken}")
            .header("Notion-Version", "2022-06-28")
            .header("Content-Type", "application/json")
            .method(
                "POST", mapper.writeValueAsString(body).toRequestBody()
            )
            .build()

        return client.newCall(startRequest).execute()
    }

    private suspend fun collectPagination(body: Map<Any, Any?> = emptyMap()): List<NotionGame> {
        val response = query(body)

        return buildList<NotionGame> {
            val json = mapper.readTree(response.body!!.byteStream())

            json["results"]
                .map { NotionGame.fromNotionPageJson(it) }
                .forEach { add(it) }

            var nextCursor = json["next_cursor"].asText().let { if (it == "null") null else it }

            while (nextCursor != null) {
                yield()
                val nextResponse = query(body + mapOf("start_cursor" to nextCursor))
                val nextJson = mapper.readTree(nextResponse.body!!.byteStream())

                nextJson["results"]
                    .map { NotionGame.fromNotionPageJson(it) }
                    .forEach { add(it) }

                nextCursor = nextJson["next_cursor"].asText().let { if (it == "null") null else it }
            }
        }
    }

    suspend fun getAllGames(): List<NotionGame> = coroutineScope {
        async(Dispatchers.IO) {
            collectPagination()
        }.await()
    }

    suspend fun getGameForNumberOfPlayersEquals(players: Int): List<NotionGame> = coroutineScope {
        async(Dispatchers.IO) {
            collectPagination(
                mapOf(
                    "filter" to mapOf(
                        "and" to listOf(
                            mapOf(
                                "property" to "Min Players",
                                "number" to mapOf(
                                    "less_than_or_equal_to" to players
                                )
                            ),
                            mapOf(
                                "property" to "Max Players",
                                "number" to mapOf(
                                    "greater_than_or_equal_to" to players
                                )
                            )
                        )
                    )
                )
            )
        }.await()
    }

    suspend fun getGameForNumberOfPlayersBetween(min: Int, max: Int): List<NotionGame> = coroutineScope {
        async(Dispatchers.IO) {
            collectPagination(
                mapOf(
                    "filter" to mapOf(
                        "and" to listOf(
                            mapOf(
                                "property" to "Min Players",
                                "number" to mapOf(
                                    "less_than_or_equal_to" to max
                                )
                            ),
                            mapOf(
                                "property" to "Max Players",
                                "number" to mapOf(
                                    "greater_than_or_equal_to" to min
                                )
                            )
                        )
                    )
                )
            )
        }.await()
    }
}