package dev.mtib.ketchup.server.aoc

import com.fasterxml.jackson.annotation.JsonProperty
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.channel.TextChannel
import dev.mtib.ketchup.bot.utils.ketchupZone
import dev.mtib.ketchup.server.KordContainer
import dev.mtib.ketchup.server.auth.AuthClient.checkAuth
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds
import dev.mtib.ketchup.bot.features.aoc.Client as AocClient
import dev.mtib.ketchup.bot.features.aoc.Client.Cache.Companion as AocCache

object AocRoutes {
    data class BenchmarkData(
        val year: Int,
        val day: Int,
        val part: Int,
        @JsonProperty("time_ms")
        val timeMs: Double
    )

    fun Routing.registerAocRoutes() {
        route("/aoc") {
            route("/input") {
                put("/{snowflake}/{year}/{day}") {
                    val snowflake = call.parameters["snowflake"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing snowflake"))
                        return@put
                    }
                    if (!checkAuth(snowflake)) {
                        return@put
                    }
                    val year = call.parameters["year"]?.toIntOrNull() ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing year"))
                        return@put
                    }
                    val day = call.parameters["day"]?.toIntOrNull() ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing day"))
                        return@put
                    }

                    val input = call.receiveText()
                    AocClient.recordInput(snowflake, year, day, input)
                    call.respond(mapOf("message" to "ok"))
                }

                get("/{snowflake}/{year}/{day}") {
                    val snowflake = call.parameters["snowflake"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing snowflake"))
                        return@get
                    }
                    if (!checkAuth(snowflake)) {
                        return@get
                    }
                    val year = call.parameters["year"]?.toIntOrNull() ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing year"))
                        return@get
                    }
                    val day = call.parameters["day"]?.toIntOrNull() ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing day"))
                        return@get
                    }

                    val input = AocClient.retrieveInput(snowflake, year, day)
                    if (input == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Input not found"))
                    } else {
                        call.respond(input)
                    }
                }
            }
            post("/announce/{aocChannelSnowflake}/{userSnowflake}/{day}") {
                val aocChannelSnowflake = call.parameters["aocChannelSnowflake"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing aocChannelSnowflake"))
                    return@post
                }
                val userSnowflake = call.parameters["userSnowflake"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing userSnowflake"))
                    return@post
                }
                val day = call.parameters["day"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing day"))
                    return@post
                }
                if (!checkAuth(userSnowflake)) {
                    return@post
                }

                KordContainer.get().let { kord ->
                    val channel = kord.getChannelOf<TextChannel>(Snowflake(aocChannelSnowflake)) ?: run {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Channel not found"))
                        return@post
                    }
                    val thread = channel.activeThreads.firstOrNull { it.name == "Day $day" } ?: run {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Thread not found"))
                        return@post
                    }
                    thread.createMessage(
                        buildString {
                            appendLine("<@!$userSnowflake> just announced:")
                            call.receiveText().lines().joinTo(this, "\n") { "> $it" }
                        }
                    )
                    call.respond(mapOf("message" to "ok", "day" to day))
                }
            }
            route("/benchmark") {
                post("/user/{snowflake}") {
                    val snowflake = call.parameters["snowflake"]!!
                    if (!checkAuth(snowflake)) {
                        return@post
                    }
                    val data = try {
                        call.receive(BenchmarkData::class)
                    } catch (e: ContentTransformationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Malformed request", "details" to e.message)
                        )
                        return@post
                    }

                    if (data.day !in 1..25 || data.part !in 1..2) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid day or part"))
                        return@post
                    }

                    val time = AocClient.recordBenchmarkResult(
                        userSnowflake = snowflake,
                        event = data.year.toString(),
                        day = data.day,
                        part = data.part,
                        timeMs = data.timeMs
                    )

                    val puzzleRelease = ZonedDateTime.of(
                        data.year, 12, data.day, 5, 0, 0, 0, ketchupZone
                    )
                    val durationSinceRelease = puzzleRelease.toInstant()
                        .until(time, ChronoUnit.SECONDS).seconds

                    if (durationSinceRelease.isNegative()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to "Benchmark recorded before puzzle release",
                                "time_until_release" to (-durationSinceRelease).toString()
                            )
                        )
                        return@post
                    }

                    call.respond(
                        mapOf(
                            "message" to "ok",
                            "data" to data,
                            "timestamp_epoch_seconds" to time.epochSecond,
                            "puzzle_release_epoch_seconds" to puzzleRelease.toEpochSecond(),
                            "time_since_puzzle_release" to durationSinceRelease.toString()
                        )
                    )
                }
                get("/user/{snowflake}") {
                    val snowflake = call.parameters["snowflake"]
                    if (!checkAuth(snowflake)) {
                        return@get
                    }
                    val start = Clock.System.now()
                    if (snowflake.isNullOrBlank()) {
                        call.respond(mapOf("error" to "Malformed snowflake"))
                        return@get
                    }
                    val userData = AocCache.getByUserSnowflake(snowflake).sortedBy {
                        it.timestamp
                    }.groupBy { it.event }.mapValues {
                        it.value.groupBy { it.day }.mapValues {
                            it.value.groupBy { it.part }.mapValues { it.value.sortedBy { it.timestamp } }
                        }
                    }.let {
                        mapOf(
                            "benchmarks" to it,
                            "user" to snowflake,
                            "timestamp_millis" to System.currentTimeMillis(),
                            "query_time_micros" to start.until(Clock.System.now(), DateTimeUnit.NANOSECOND)
                                .toBigDecimal().movePointLeft(3)
                        )
                    }
                    call.respond(userData)
                }
                delete("/user/{snowflake}/{timestamp}") {
                    val snowflake = call.parameters["snowflake"]
                    if (!checkAuth(snowflake)) {
                        return@delete
                    }
                    val timestamp = call.parameters["timestamp"]?.toLongOrNull()
                    if (timestamp == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Malformed timestamp"))
                        return@delete
                    }
                    val count = AocClient.deleteByUserSnowflakeAndTimestamp(snowflake, timestamp)
                    call.respond(mapOf("message" to "ok", "deleted" to count))
                }
            }
        }
    }
}