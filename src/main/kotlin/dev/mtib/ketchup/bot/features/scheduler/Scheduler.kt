package dev.mtib.ketchup.bot.features.scheduler

import dev.kord.core.Kord
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.bot.features.scheduler.interfaces.ScheduledAction.ActionNotDueException
import dev.mtib.ketchup.bot.features.scheduler.interfaces.ScheduledAction.ActionNotDueException.InteractionNotDueException
import dev.mtib.ketchup.bot.features.scheduler.interfaces.ScheduledAction.ActionNotDueException.MessageNotDueException
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduledInteractionsTable
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduledMessagesTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

object Scheduler : Feature {
    private val logger = KotlinLogging.logger { }

    private const val INTERVAL_MS: Long = 60 * 1000
    private lateinit var kord: Kord
    private lateinit var job: Job

    override fun cancel() {
        job.cancel()
    }

    override fun register(kord: Kord) {
        this.kord = kord
        this.job = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                run()
                delay(INTERVAL_MS)
            }
        }
    }

    private suspend fun run() {
        logger.debug { "Scheduler running" }
        val db = getAnywhere<Database>()
        db.transaction {
            listOf(
                // Editable, user may have changed due time
                ScheduledMessagesTable.getUnsentMessages(),
                // Not editable, so no need to check for updates
                ScheduledInteractionsTable.getDueUnsentMessages()
            ).flatten()
        }
            .asFlow()
            .onEach {
                try {
                    it.handleMessage(kord).onSome {
                        db.transaction(it)
                    }
                } catch (e: ActionNotDueException) {
                    when (e) {
                        is MessageNotDueException -> {
                            db.transaction {
                                logger.debug { "Message not due: ${e.scheduledTask.messageIdentification.messageId}" }
                            }
                        }

                        is InteractionNotDueException -> {
                            db.transaction {
                                logger.debug { "Interaction not due: ${e.scheduledInteraction.id}" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to handle message: $it" }
                }
            }.collect()
    }

}