package dev.mtib.ketchup.bot.utils

import org.koin.mp.KoinPlatform

inline fun <reified T : Any> getAnywhere(): T {
    return KoinPlatform.getKoin().get<T>()
}

inline fun <reified T : Any> getAllAnywhere(): List<T> {
    return KoinPlatform.getKoin().getAll<T>()
}
