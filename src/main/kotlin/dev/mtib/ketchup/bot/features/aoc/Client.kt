package dev.mtib.ketchup.bot.features.aoc

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import dev.mtib.ketchup.bot.utils.ketchupObjectMapper
import dev.mtib.ketchup.bot.utils.ketchupZone
import dev.mtib.ketchup.bot.utils.readOrNull
import dev.mtib.ketchup.bot.utils.write
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import okhttp3.OkHttpClient
import java.time.OffsetDateTime
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

object Client {
    private val httpClient = OkHttpClient().newBuilder().followRedirects(false).build()
    private val cacheMutex = Mutex()
    private const val MAX_AGE_SECONDS = 900
    private const val CACHE_FILE = "aoc.json"
    private val logger = KotlinLogging.logger {}

    class Cache(
        val items: List<CacheItem> = emptyList(),
        val listeners: List<Listener> = emptyList(),
        val benchmarks: List<BenchmarkReport> = emptyList(),
    ) {
        class CacheItem(
            val timestamp: java.time.Instant,
            val data: Leaderboard,
            val cookie: String,
        )

        data class Listener(
            val snowflake: String,
            val event: String,
            val ownerId: Long,
            val cookie: String,
        )

        class BenchmarkReport(
            val userSnowflake: String,
            val event: String,
            val day: Int,
            val part: Int,
            val timeMs: Double,
            val timestamp: java.time.Instant,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Leaderboard(
        val event: String,
        @JsonProperty("owner_id")
        val ownerId: Long,
        val members: Map<String, Member>
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Member(
            @JsonProperty("local_score")
            val localScore: Int,
            val name: String,
            val id: Long,
            @JsonProperty("last_star_ts")
            val lastStarTs: Long,
            @JsonProperty("global_score")
            val globalScore: Int,
            val stars: Int,
        )

        @JsonIgnore
        fun isCurrent(): Boolean {
            val now = OffsetDateTime.now(ketchupZone)!!
            return now.year.toString() == event && now.dayOfMonth <= 26 && now.monthValue == 12
        }
    }

    class RedirectException : Exception("Redirected")

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

    suspend fun getListeners(): List<Cache.Listener> {
        cacheMutex.withLock {
            val cachePath = Path(CACHE_FILE)
            val cache = cachePath.readOrNull<Cache>() ?: Cache()
            return cache.listeners
        }
    }

    suspend fun addListener(snowflake: String, event: String, ownerId: Long, cookie: String) {
        cacheMutex.withLock {
            val cachePath = Path(CACHE_FILE)
            val cache = cachePath.readOrNull<Cache>() ?: Cache()
            val newCache = Cache(
                cache.items,
                cache.listeners + Cache.Listener(snowflake, event, ownerId, cookie)
            )
            cachePath.write(newCache)
        }
    }

    suspend fun removeListener(listener: Cache.Listener) {
        cacheMutex.withLock {
            val cachePath = Path(CACHE_FILE)
            val cache = cachePath.readOrNull<Cache>() ?: Cache()
            val newCache = Cache(
                cache.items,
                cache.listeners.filter { it.snowflake != listener.snowflake || it.event != listener.event || it.ownerId != listener.ownerId }
            )
            cachePath.write(newCache)
        }
    }

    suspend fun recordBenchmarkResult(userSnowflake: String, event: String, day: Int, part: Int, timeMs: Double) {
        cacheMutex.withLock {
            val cachePath = Path(CACHE_FILE)
            val cache = cachePath.readOrNull<Cache>() ?: Cache()
            val newCache = Cache(
                items = cache.items,
                listeners = cache.listeners,
                benchmarks = cache.benchmarks + Cache.BenchmarkReport(
                    userSnowflake = userSnowflake,
                    event = event,
                    day = day,
                    part = part,
                    timeMs = timeMs,
                    timestamp = Clock.System.now().toJavaInstant()
                )
            )
            cachePath.write(newCache)
        }
    }

    /**
     * Maps indexed by event, day, part, userSnowflake
     */
    suspend fun getBenchmarkResults(): Map<String, Map<Int, Map<Int, Map<String, List<Cache.BenchmarkReport>>>>> {
        return cacheMutex.withLock {
            val cachePath = Path(CACHE_FILE)
            cachePath.readOrNull<Cache>() ?: Cache()
        }.let { cache ->
            cache.benchmarks.groupBy { it.event }.mapValues {
                it.value.groupBy { it.day }.mapValues {
                    it.value.groupBy { it.part }.mapValues {
                        it.value.groupBy { it.userSnowflake }.mapValues {
                            it.value.sortedBy { it.timestamp }
                        }
                    }
                }
            }
        }
    }

    suspend fun getLeaderboard(year: Int, ownerId: Long, cookie: String): Leaderboard {
        cacheMutex.withLock {
            val cachePath = Path(CACHE_FILE)
            val cache = cachePath.readOrNull<Cache>() ?: Cache()
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
                val newCache =
                    Cache(cache.items.filter { it.data.event != year.toString() && it.data.ownerId != ownerId } + Cache.CacheItem(
                        Clock.System.now().toJavaInstant(),
                        item,
                        cookie
                    ))
                cachePath.write(newCache)
            }

            return item
        }
    }
}