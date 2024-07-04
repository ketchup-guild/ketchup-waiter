package dev.mtib.ketchup.bot.features.scheduler

import dev.kord.common.DiscordTimestampStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.channel.TextChannel
import dev.mtib.ketchup.bot.commands.schedule.ScheduleMessageCommand
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduleTable
import dev.mtib.ketchup.bot.storage.Database
import dev.mtib.ketchup.bot.utils.getAnywhere
import dev.mtib.ketchup.bot.utils.toMessageFormat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.time.Instant

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
        val tasks = db.transaction {
            ScheduleTable.getUnsentMessages().toList()
        }
        tasks.asFlow().onEach {
            try {
                val update = handleMessage(it)
                db.transaction(update)
            } catch (e: TaskNotDueException) {
                logger.debug { "Message not due: ${e.scheduledTask.messageIdentification.messageId}" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to handle message: ${it.messageIdentification.messageId}" }
            }
        }.collect()
    }

    class TaskNotDueException(
        val scheduledTask: ScheduleTable.Schedule
    ) : Exception("Message not due")

    private suspend fun handleMessage(task: ScheduleTable.Schedule): Transaction.() -> Unit {
        val target = Target.fromULong(kord, task.targetId)
        val source = task.messageIdentification.getFromSupplier(kord.defaultSupplier)

        val specs = ScheduleMessageCommand.extractMessageSpecs(source)

        val correctionSteps = buildList<ScheduleTable.(UpdateStatement) -> Unit> {
            if (specs.target.value != task.targetId) {
                add {
                    it[targetId] = specs.target.value
                }
            }
            if (specs.time != task.time) {
                add {
                    it[time] = specs.time
                }
            }
        }

        if (correctionSteps.isNotEmpty()) {
            return {
                ScheduleTable.update2(task.messageIdentification.messageId) {
                    correctionSteps.forEach { correction ->
                        this.correction(it)
                    }
                }
            }
        }

        if (task.time.isAfter(Instant.now())) {
            throw TaskNotDueException(task)
        }

        target.send(
            """
            ${specs.content}
            
            > Scheduled by ${source.author!!.mention} in ${source.channel.mention} ${
                task.createdAt.toMessageFormat(
                    DiscordTimestampStyle.RelativeTime
                )
            } to be sent ${task.time.toMessageFormat(DiscordTimestampStyle.RelativeTime)}
        """.trimIndent()
        )

        return {
            ScheduleTable.markAsSent(task.messageIdentification.messageId)
        }
    }

    sealed class Target {
        data class User(val user: dev.kord.core.entity.User) : Target() {
            override val mention: String
                get() = user.mention

            override suspend fun send(content: String) {
                user.getDmChannel().createMessage(content)
            }
        }

        data class Channel(val channel: dev.kord.core.entity.channel.Channel) : Target() {
            override val mention: String
                get() = channel.mention

            override suspend fun send(content: String) {
                channel.asChannelOf<TextChannel>().createMessage(content)
            }
        }

        companion object {
            suspend fun fromULong(kord: Kord, id: ULong): Target {
                val user = kord.getUser(Snowflake(id))
                if (user != null) {
                    return User(user)
                }
                val channel = kord.getChannel(Snowflake(id))
                if (channel != null) {
                    return Channel(channel)
                }
                throw IllegalArgumentException("No user or channel found with id $id")
            }
        }

        abstract val mention: String
        abstract suspend fun send(content: String)
    }
}