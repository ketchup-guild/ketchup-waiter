package dev.mtib.ketchup.bot.utils

import dev.kord.core.entity.User
import dev.mtib.ketchup.bot.KetchupBot

val User?.isGod: Boolean
    get() = this?.id in KetchupBot.GOD_IDS

