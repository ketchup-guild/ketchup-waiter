package dev.mtib.ketchup.bot.storage

import dev.mtib.ketchup.bot.features.ketchupRank.storage.KetchupGivingTable
import dev.mtib.ketchup.bot.features.ketchupRank.storage.KetchupRankTable
import dev.mtib.ketchup.bot.features.openai.storage.DalleTrackingTable
import dev.mtib.ketchup.bot.features.openai.storage.GptTrackingTable
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduledInteractionsTable
import dev.mtib.ketchup.bot.features.scheduler.storage.ScheduledMessagesTable
import dev.mtib.ketchup.bot.features.subscriptions.broadcast.storage.SubscriptionsTable
import dev.mtib.ketchup.bot.features.subscriptions.reactions.storage.ReactionSubscriptionsTable
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

class Database {
    companion object {
        private val logging = System.getenv("LOG_DB") == "true"
        private val logger = KotlinLogging.logger {}
        private val database by lazy {
            val db = org.jetbrains.exposed.sql.Database.connect(
                "jdbc:sqlite:ketchup-db.sqlite",
                "org.sqlite.JDBC"
            )
            logger.info { "Connected to database" }
            transaction(db) {
                SchemaUtils.createMissingTablesAndColumns(
                    KetchupRankTable,
                    KetchupGivingTable,
                    GptTrackingTable,
                    DalleTrackingTable,
                    SubscriptionsTable,
                    ScheduledMessagesTable,
                    ReactionSubscriptionsTable,
                    ScheduledInteractionsTable
                )
            }
            logger.info { "Database ready" }
            db
        }
    }

    fun <T> transaction(body: Transaction.() -> T): T = transaction(database) {
        if (logging) {
            addLogger(StdOutSqlLogger)
        }
        body()
    }
}