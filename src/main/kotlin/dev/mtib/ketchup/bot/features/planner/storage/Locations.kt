package dev.mtib.ketchup.bot.features.planner.storage

import dev.kord.common.entity.Snowflake
import dev.mtib.ketchup.bot.utils.orThrow

data class Locations(
    val upcomingEventsSnowflake: Snowflake,
    val eventArchiveSnowflake: Snowflake,
    val ideaChannelSnowflake: Snowflake,
    val announcementChannelSnowflake: Snowflake,
) {
    companion object {
        private const val UPCOMING_EVENTS = "1173059279436132442"
        private const val EVENT_ARCHIVE = "1172585839734296586"

        private const val IDEA_CHANNEL_ENV_VAR = "KETCHUP_IDEA_CHANNEL"
        private const val ANNOUNCEMENT_CHANNEL_ENV_VAR = "KETCHUP_ANNOUNCEMENT_CHANNEL"

        /**
         * Retrieves the locations from the environment variables.
         *
         * @throws IllegalStateException if the environment variables are not set
         */
        fun fromEnvironment(): Locations {
            return Locations(
                Snowflake(UPCOMING_EVENTS),
                Snowflake(EVENT_ARCHIVE),
                Snowflake(
                    System.getenv(IDEA_CHANNEL_ENV_VAR)
                        .orThrow { "$IDEA_CHANNEL_ENV_VAR environment variable not set" }),
                Snowflake(
                    System.getenv(ANNOUNCEMENT_CHANNEL_ENV_VAR)
                        .orThrow { "$ANNOUNCEMENT_CHANNEL_ENV_VAR environment variable not set" }),
            )
        }
    }
}