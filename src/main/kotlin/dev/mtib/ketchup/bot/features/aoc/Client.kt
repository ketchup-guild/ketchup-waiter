package dev.mtib.ketchup.bot.features.aoc

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import dev.mtib.ketchup.bot.utils.ketchupObjectMapper
import dev.mtib.ketchup.bot.utils.ketchupZone
import dev.mtib.ketchup.common.RedisClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import okhttp3.OkHttpClient
import org.json.JSONArray
import redis.clients.jedis.json.Path2
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds

object Client {
    private val httpClient = OkHttpClient().newBuilder().followRedirects(false).build()
    private val cacheMutex = Mutex()
    private const val MAX_AGE_SECONDS = 900
    private val logger = KotlinLogging.logger {}

    private val redisCache by lazy {
        System.getenv("REDIS_AOC_CACHE") ?: error("Missing REDIS_AOC_CACHE, like 'aoc:ketchup:cache'")
    }

    class Cache @JsonCreator private constructor(
        @JsonProperty("items")
        val items: List<CacheItem> = emptyList(),
        @JsonProperty("listeners")
        val listeners: List<Listener> = emptyList(),
        @JsonProperty("benchmarks")
        val benchmarks: List<BenchmarkReport> = emptyList(),
    ) {
        class CacheItem(
            @JsonProperty("timestamp")
            val timestamp: java.time.Instant,
            @JsonProperty("data")
            val data: Leaderboard,
            @JsonProperty("cookie")
            val cookie: String,
        )

        data class Listener(
            @JsonProperty("snowflake")
            val snowflake: String,
            @JsonProperty("event")
            val event: String,
            @JsonProperty("owner_id")
            val ownerId: Long,
            @JsonProperty("cookie")
            val cookie: String,
        )

        class BenchmarkReport(
            @JsonProperty("user_snowflake")
            val userSnowflake: String,
            @JsonProperty("event")
            val event: String,
            @JsonProperty("day")
            val day: Int,
            @JsonProperty("part")
            val part: Int,
            @JsonProperty("time_ms")
            val timeMs: Double,
            @JsonProperty("timestamp")
            val timestamp: java.time.Instant,
        )

        companion object {
            @JsonIgnore
            fun empty(): Cache = Cache()

            @JsonIgnore
            fun get(): Cache = run {
                RedisClient.pool.jsonGet(redisCache)?.let {
                    ketchupObjectMapper.convertValue(it)
                } ?: empty()
            }

            private val tr = object : TypeReference<List<BenchmarkReport>>() {}

            @JsonIgnore
            fun getByUserSnowflake(snowflake: String): List<BenchmarkReport> {
                return RedisClient.pool.jsonGet(redisCache, Path2("$.benchmarks[?(@.user_snowflake == '$snowflake')]"))
                    ?.let {
                        if (it !is JSONArray) {
                            error("Expected JSONArray, got ${it::class.simpleName}")
                        }
                        ketchupObjectMapper.convertValue(it.toList(), tr)
                    } ?: emptyList()
            }
        }

        @JsonIgnore
        fun save() {
            RedisClient.pool.jsonSet(redisCache, ketchupObjectMapper.writeValueAsString(this))
        }

        @JsonIgnore
        fun copy(
            items: List<CacheItem> = this.items,
            listeners: List<Listener> = this.listeners,
            benchmarks: List<BenchmarkReport> = this.benchmarks,
        ) = Cache(items, listeners, benchmarks)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Leaderboard(
        @JsonProperty("event")
        val event: String,
        @JsonProperty("owner_id")
        val ownerId: Long,
        @JsonProperty("members")
        val members: Map<String, Member>
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Member(
            @JsonProperty("local_score")
            val localScore: Int,
            @JsonProperty("name")
            val name: String,
            @JsonProperty("id")
            val id: Long,
            @JsonProperty("last_star_ts")
            val lastStarTs: Long,
            @JsonProperty("global_score")
            val globalScore: Int,
            @JsonProperty("stars")
            val stars: Int,
        )

        @JsonIgnore
        fun isCurrent(): Boolean {
            val now = OffsetDateTime.now(ketchupZone)!!
            return now.year.toString() == event && now.dayOfMonth <= 26 && now.monthValue == 12
        }
    }

    class RedirectException : Exception("Redirected")

    suspend fun getListeners(): List<Cache.Listener> = cacheMutex.withLock {
        Cache.get()
    }.listeners

    suspend fun addListener(snowflake: String, event: String, ownerId: Long, cookie: String) {
        val newListener = Cache.Listener(snowflake, event, ownerId, cookie)
        cacheMutex.withLock {
            Cache.get().run {
                copy(
                    listeners = listeners.filter {
                        it.snowflake != newListener.snowflake || it.event != newListener.event || it.ownerId != newListener.ownerId
                    } + newListener
                )
            }.save()
        }
    }

    suspend fun removeListener(listener: Cache.Listener) {
        cacheMutex.withLock {
            Cache.get().run {
                copy(
                    listeners = listeners.filter { it.snowflake != listener.snowflake || it.event != listener.event || it.ownerId != listener.ownerId }
                )
            }.save()
        }
    }

    suspend fun recordBenchmarkResult(userSnowflake: String, event: String, day: Int, part: Int, timeMs: Double) {
        cacheMutex.withLock {
            Cache.get().run {
                copy(
                    benchmarks = benchmarks + Cache.BenchmarkReport(
                        userSnowflake = userSnowflake,
                        event = event,
                        day = day,
                        part = part,
                        timeMs = timeMs,
                        timestamp = Clock.System.now().toJavaInstant()
                    )
                )
            }.save()
        }
    }

    /**
     * Maps indexed by event, day, part, userSnowflake
     */
    suspend fun getBenchmarkResults(): Map<String, Map<Int, Map<Int, Map<String, List<Cache.BenchmarkReport>>>>> {
        return cacheMutex.withLock {
            Cache.get()
        }.let { cache ->
            cache.benchmarks.groupBy { it.event }.mapValues { (_, byEvent) ->
                byEvent.groupBy { it.day }.mapValues { (_, byDay) ->
                    byDay.groupBy { it.part }.mapValues { (_, byPart) ->
                        byPart.groupBy { it.userSnowflake }.mapValues { (_, byUser) ->
                            byUser.sortedBy { it.timestamp }
                        }
                    }
                }
            }
        }
    }

    private fun fetchLoaderboard(year: Int, ownerId: Long, cookie: String): Leaderboard {
        logger.info { "Fetching leaderboard $ownerId for $year" }
        val request = okhttp3.Request.Builder()
            .url("https://adventofcode.com/$year/leaderboard/private/view/$ownerId.json")
            .header("Cookie", "session=$cookie;")
            .build()
        val response = httpClient.newCall(request).execute()
        if (response.isRedirect) {
            logger.error { "Failed to fetch leaderboard $ownerId for $year: Redirected" }
            throw RedirectException()
        }
        return ketchupObjectMapper.readValue(response.body!!.string())
    }

    suspend fun getLeaderboard(year: Int, ownerId: Long, cookie: String): Leaderboard {
        cacheMutex.withLock {
            val cache = Cache.get()
            cache.items.find { it.data.event == year.toString() && it.data.ownerId == ownerId }?.let { item ->
                if (item.timestamp.toKotlinInstant().plus(MAX_AGE_SECONDS.seconds) > Clock.System.now()) {
                    logger.debug { "Cache hit for $ownerId $year (very fresh)" }
                    return item.data
                }
                val now = OffsetDateTime.now(ketchupZone)!!
                if (now.year > year) {
                    logger.debug { "Cache hit for $ownerId $year (static)" }
                    return item.data
                }
            }
            logger.debug { "Cache miss for $ownerId $year" }
            val item = fetchLoaderboard(year, ownerId, cookie).also { item ->
                cache.copy(
                    items = cache.items.filter { it.data.event != year.toString() && it.data.ownerId != ownerId } + Cache.CacheItem(
                        Clock.System.now().toJavaInstant(),
                        item,
                        cookie
                    )
                ).save()
            }

            return item
        }
    }
}