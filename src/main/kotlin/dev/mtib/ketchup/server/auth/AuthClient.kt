package dev.mtib.ketchup.server.auth

import dev.mtib.ketchup.common.RedisClient
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object AuthClient {
    private const val TOKEN_KEY = "auth:ketchup:tokens"
    val validity = 365.days

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createToken(snowflake: String): String {
        val newToken = Uuid.random().toString()
        withContext(RedisClient.dispatcher) {
            RedisClient.pool.pipelined().apply {
                hset(
                    TOKEN_KEY,
                    snowflake,
                    newToken
                )
                hexpire(
                    TOKEN_KEY,
                    365.days.inWholeSeconds,
                    snowflake
                )
            }.sync()
        }
        return newToken
    }

    private suspend fun checkToken(snowflake: String, token: String): Boolean {
        return withContext(RedisClient.dispatcher) {
            RedisClient.pool.hget(TOKEN_KEY, snowflake) == token
        }
    }

    @OptIn(ExperimentalContracts::class)
    suspend fun RoutingContext.checkAuth(snowflake: String?): Boolean {
        contract {
            returns(true) implies (snowflake != null)
        }
        if (snowflake == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing identity"))
            return false
        }
        val token = (call.request.headers["Authorization"].also {
            if (it == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing Authorization header"))
                return false
            }
        }!!).let {
            Regex("Bearer (.+)").find(it)?.groupValues?.get(1).also {
                if (it == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Invalid Authorization header: expected 'Bearer <token>'")
                    )
                    return false
                }
            }!!
        }
        if (!checkToken(snowflake, token)) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
            return false
        }

        return true
    }
}