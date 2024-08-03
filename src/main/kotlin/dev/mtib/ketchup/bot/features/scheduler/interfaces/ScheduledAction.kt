package dev.mtib.ketchup.bot.features.scheduler.interfaces

import arrow.core.Option
import dev.kord.core.Kord
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduledInteractionsTable.ScheduledInteraction
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduledMessagesTable.ScheduledMessage
import org.jetbrains.exposed.sql.Transaction

interface ScheduledAction {
    /**
     * Handles the scheduled action
     * @param kord the kord instance
     * @throws ActionNotDueException if the action is not due yet
     * @return a function that will be executed after the action is handled
     */
    suspend fun handleMessage(kord: Kord): Option<Transaction.() -> Unit>

    fun actionDue(): Boolean

    sealed class ActionNotDueException(
        val type: Class<out ScheduledAction>
    ) : Exception("${type.simpleName} action is not due yet") {
        class MessageNotDueException(
            val scheduledTask: ScheduledMessage
        ) : ActionNotDueException(ScheduledMessage::class.java)

        class InteractionNotDueException(
            val scheduledInteraction: ScheduledInteraction
        ) : ActionNotDueException(ScheduledInteraction::class.java)
    }
}