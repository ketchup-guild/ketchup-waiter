package dev.mtib.ketchup.server.auth

import dev.mtib.ketchup.bot.utils.ketchupObjectMapper
import dev.mtib.ketchup.common.RedisClient
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.json.JSONArray
import org.json.JSONObject
import redis.clients.jedis.json.JsonSetParams
import redis.clients.jedis.json.Path2
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object AuthClient {
    private const val TOKEN_KEY = "auth:ketchup:tokens"

    @OptIn(ExperimentalUuidApi::class)
    fun createToken(snowflake: String): String {
        val newToken = Uuid.random().toHexString()
        RedisClient.pool.pipelined().apply {
            jsonSet(
                TOKEN_KEY,
                Path2.ROOT_PATH,
                JSONObject().put(snowflake, JSONArray()),
                JsonSetParams.jsonSetParams().nx()
            )
            jsonArrAppend(
                TOKEN_KEY,
                Path2("$[${ketchupObjectMapper.writeValueAsString(snowflake)}]"),
                ketchupObjectMapper.writeValueAsString(newToken)
            )
        }.sync()
        return newToken
    }

    fun checkToken(snowflake: String, token: String): Boolean {
        val tokens =
            RedisClient.pool.jsonGet(TOKEN_KEY, Path2("$[${ketchupObjectMapper.writeValueAsString(snowflake)}]")).let {
                if (it !is JSONArray) return false
                it
            }.toList()
        return tokens.filterIsInstance<List<String>>().flatten().contains(token)
    }

    suspend fun RoutingContext.checkAuth(snowflake: String?): Boolean {
        if (snowflake == null) {
            call.respond(mapOf("error" to "Missing identity"))
            call.response.status(HttpStatusCode.Unauthorized)
            return false
        }
        val token = (call.request.headers["Authorization"].also {
            if (it == null) {
                call.respond(mapOf("error" to "Missing Authorization header"))
                call.response.status(HttpStatusCode.Unauthorized)
                return false
            }
        }!!).let {
            Regex("Bearer (.+)").find(it)?.groupValues?.get(1).also {
                if (it == null) {
                    call.respond(mapOf("error" to "Invalid Authorization header: expected 'Bearer <token>'"))
                    call.response.status(HttpStatusCode.Unauthorized)
                    return false
                }
            }!!
        }
        if (!checkToken(snowflake, token)) {
            call.respond(mapOf("error" to "Invalid token"))
            call.response.status(HttpStatusCode.Unauthorized)
            return false
        }

        return true
    }
}