package dev.mtib.ketchup.bot.features.subscriptions.reactions

import dev.kord.core.entity.User
import dev.mtib.ketchup.bot.features.subscriptions.reactions.storage.ReactionSubscriptionsTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere

object ReactionSubscriptions {
    val db by lazy { getAnywhere<Database>() }
    fun subscribe(user: User) {
        return db.transaction { ReactionSubscriptionsTable.subscribe(user.id.value) }
    }

    fun unsubscribe(user: User) {
        return db.transaction { ReactionSubscriptionsTable.unsubscribe(user.id.value) }
    }

    fun isSubscribed(user: User): Boolean {
        return db.transaction { ReactionSubscriptionsTable.isSubscribed(user.id.value) }
    }
}