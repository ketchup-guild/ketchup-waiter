package dev.mtib.ketchup.server

import dev.kord.core.Kord

object KordContainer {
    private lateinit var _kord: Kord

    suspend fun get(): Kord {
        if (!::_kord.isInitialized) {
            _kord = Kord(System.getenv("KETCHUP_BOT_TOKEN") ?: error("Missing KETCHUP_BOT_TOKEN"))
        }
        return _kord
    }

    suspend fun close() {
        if (::_kord.isInitialized) {
            _kord.shutdown()
        }
    }
}