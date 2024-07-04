package dev.mtib.ketchup.bot.features

import dev.kord.core.Kord

interface Feature {
    fun register(kord: Kord)
    fun cancel() {}
}