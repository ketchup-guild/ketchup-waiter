package dev.mtib.ketchup.server.aoc

import dev.mtib.ketchup.server.auth.AuthClient.checkAuth
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import dev.mtib.ketchup.bot.features.aoc.Client.Cache.Companion as AocCache

object AocRoutes {
    fun Routing.registerAocRoutes() {
        route("/aoc") {
            route("/benchmark") {
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
            }
        }
    }
}