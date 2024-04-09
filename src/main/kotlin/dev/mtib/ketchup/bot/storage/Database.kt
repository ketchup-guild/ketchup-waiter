package dev.mtib.ketchup.bot.storage

import dev.mtib.ketchup.bot.features.ketchupRank.storage.KetchupGivingTable
import dev.mtib.ketchup.bot.features.ketchupRank.storage.KetchupRankTable
import mu.KotlinLogging
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