package dev.mtib.ketchup.bot.utils

import java.util.*

fun <T : Any> rememberOnce(block: () -> T): () -> T {
    var result = Optional.empty<T>()
    return {
        if (result.isPresent.not()) {
            result = Optional.of(block())
        }
        result.get()
    }
}

fun <C, T : Any> rememberOnce(block: (context: C) -> T): (context: C) -> T {
    var result = Optional.empty<T>()
    return { context ->
        if (result.isPresent.not()) {
            result = Optional.of(block(context))
        }
        result.get()
    }
}
