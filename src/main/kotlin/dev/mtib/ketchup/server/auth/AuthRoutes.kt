package dev.mtib.ketchup.server.auth

import dev.mtib.ketchup.server.auth.AuthClient.checkAuth
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object AuthRoutes {
    fun Routing.registerAuthRoutes() {
        route("/auth") {
            post("/check/{snowflake}") {
                val snowflake = call.parameters["snowflake"] ?: run {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respond(mapOf("error" to "Missing snowflake"))
                    return@post
                }
                if (!checkAuth(snowflake)) {
                    return@post
                }
                call.respond(mapOf("success" to true))
            }
        }
    }
}