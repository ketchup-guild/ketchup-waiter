package dev.mtib.ketchup.server.auth

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import io.ktor.server.response.*
import io.ktor.server.routing.*

object AuthRoutes {
    private var kord: Kord? = null

    fun Routing.registerAuthRoutes() {
        route("/auth") {
            post("/get-token/{snowflake}") {
                val snowflake = call.parameters["snowflake"].also {
                    if (it.isNullOrBlank()) {
                        call.respond(mapOf("error" to "Invalid snowflake"))
                        return@post
                    }
                }!!

                if (kord == null) {
                    kord = Kord(System.getenv("KETCHUP_BOT_TOKEN").also {
                        if (it.isNullOrBlank()) {
                            call.respond(mapOf("error" to "Bot token not found"))
                            return@post
                        }
                    }!!)
                }

                val user = kord!!.getUser(Snowflake(snowflake)).also {
                    if (it == null) {
                        call.respond(mapOf("error" to "User not found"))
                        return@post
                    }
                }!!

                val token = AuthClient.createToken(snowflake)
                user.getDmChannel().createMessage {
                    content = """
                        Hi there, it looks like you just requested an access token to use the ketchup bot API.
                        Your access token is `$token`.
                        
                        If this wasn't you, please ignore this message.
                    """.trimIndent()
                }

                call.respond(mapOf("message" to "ok"))
            }
        }
    }
}