package dev.mtib.ketchup.server

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import dev.mtib.ketchup.server.aoc.AocRoutes.registerAocRoutes
import dev.mtib.ketchup.server.auth.AuthRoutes.registerAuthRoutes
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

object Server {
    private const val KETCHUP_SERVER_PORT_ENV = "KETCHUP_SERVER_PORT"
    private const val KETCHUP_SERVER_DEFAULT_PORT = 8505
    private val port = System.getenv(KETCHUP_SERVER_PORT_ENV)?.toInt() ?: KETCHUP_SERVER_DEFAULT_PORT
    private val logger = KotlinLogging.logger {}
    fun start() {
        logger.info { "starting" }
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                jackson() {
                    registerModules(JavaTimeModule(), kotlinModule())
                }
            }
            routing {
                registerAocRoutes()
                registerAuthRoutes()
            }
        }.start(wait = true)
        logger.info { "shutting down" }
    }
}