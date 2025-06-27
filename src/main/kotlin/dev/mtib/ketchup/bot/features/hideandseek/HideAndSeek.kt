package dev.mtib.ketchup.bot.features.hideandseek

import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.mtib.ketchup.bot.features.Feature
import dev.mtib.ketchup.bot.utils.getEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

object HideAndSeek : Feature {
    private val logger = KotlinLogging.logger {}
    private lateinit var kord: Kord
    private var gameCounter = 0
    private val games = ConcurrentHashMap<Int, Game>()

    // Environment variables for configuration
    private val hidingPhaseDuration: Duration by lazy {
        try {
            Duration.ofMinutes(getEnv("HIDE_AND_SEEK_HIDING_PHASE_MINUTES").toLong())
        } catch (e: Exception) {
            Duration.ofMinutes(30)
        }
    }
    private val questionInterval: Duration by lazy {
        try {
            Duration.ofMinutes(getEnv("HIDE_AND_SEEK_QUESTION_INTERVAL_MINUTES").toLong())
        } catch (e: Exception) {
            Duration.ofMinutes(15)
        }
    }
    private val shortenedQuestionInterval: Duration by lazy {
        try {
            Duration.ofMinutes(getEnv("HIDE_AND_SEEK_SHORTENED_QUESTION_INTERVAL_MINUTES").toLong())
        } catch (e: Exception) {
            Duration.ofMinutes(5)
        }
    }

    override fun register(kord: Kord) {
        this.kord = kord
        logger.info { "Hide and Seek feature registered" }
    }

    override fun cancel() {
        games.values.forEach { it.cancel() }
        games.clear()
    }

    suspend fun startGame(message: Message) {
        val gameId = ++gameCounter
        val game = Game(gameId, message.getGuild()!!, kord)
        games[gameId] = game

        message.reply {
            content = """
                # Hide and Seek Game #$gameId

                Game started! Add players with `/hns player @mention team`

                Game ID: $gameId
            """.trimIndent()
        }
    }

    suspend fun addPlayer(message: Message, playerMention: String, team: String) {
        val game = getCurrentGame(message) ?: return

        val userId = extractUserId(playerMention)
        if (userId == null) {
            message.reply {
                content = "Invalid user mention. Please use the format `@username`."
            }
            return
        }

        val user = try {
            kord.getUser(userId)
        } catch (e: Exception) {
            message.reply {
                content = "Could not find user with ID $userId."
            }
            return
        }

        if (user == null) {
            message.reply {
                content = "Could not find user with ID $userId."
            }
            return
        }

        game.addPlayer(user, team)
        message.reply {
            content = "Added ${user.mention} to team $team."
        }
    }

    suspend fun shuffleTeams(message: Message) {
        val game = getCurrentGame(message) ?: return

        if (game.players.isEmpty()) {
            message.reply {
                content = "No players added yet. Add players with `/hns player @mention team`."
            }
            return
        }

        val teams = game.players.values.toSet()
        if (teams.size < 2) {
            message.reply {
                content = "At least 2 teams are required to play."
            }
            return
        }

        game.shuffleTeams()

        message.reply {
            content = buildString {
                appendLine("# Teams Assigned")
                appendLine()
                appendLine("**Hiding Team: ${game.hidingTeam}**")
                appendLine(game.getPlayersInTeam(game.hidingTeam!!).joinToString(", ") { it.mention })
                appendLine()
                appendLine("**Seeking Teams:**")
                game.seekingTeams.forEach { team ->
                    appendLine("Team $team: ${game.getPlayersInTeam(team).joinToString(", ") { it.mention }}")
                }
                appendLine()
                appendLine("Start hiding phase with `/hns hide`")
            }
        }
    }

    suspend fun startHiding(message: Message) {
        val game = getCurrentGame(message) ?: return

        if (game.hidingTeam == null) {
            message.reply {
                content = "Teams have not been assigned yet. Use `/hns shuffle` first."
            }
            return
        }

        message.reply {
            content = """
                # Hide and Seek Game #${game.id}

                Setting up channels and starting the hiding phase...
            """.trimIndent()
        }

        game.startHidingPhase(message)
    }

    suspend fun teamFound(message: Message, teamName: String) {
        val game = getCurrentGame(message) ?: return

        if (!game.isActive) {
            message.reply {
                content = "No active game in progress."
            }
            return
        }

        if (!game.seekingTeams.contains(teamName)) {
            message.reply {
                content = "Team $teamName is not a seeking team."
            }
            return
        }

        if (game.foundTeams.contains(teamName)) {
            message.reply {
                content = "Team $teamName has already found the hiding team."
            }
            return
        }

        game.markTeamAsFound(teamName)

        message.reply {
            content = buildString {
                appendLine("# Team $teamName Found the Hiders!")
                appendLine()
                appendLine("Team $teamName has found the hiding team.")
                appendLine()
                if (game.allTeamsFound()) {
                    appendLine("All teams have found the hiding team. Game over!")
                    appendLine()
                    appendLine("**Final Rankings:**")
                    game.foundTeams.forEachIndexed { index, team ->
                        val foundTime = game.foundTimes[team]
                        val duration = Duration.between(game.seekingStartTime, foundTime)
                        val minutes = duration.toMinutes()
                        val seconds = duration.seconds % 60
                        appendLine("${index + 1}. Team $team - Found in ${minutes}m ${seconds}s")
                    }
                } else {
                    appendLine("Remaining teams can now ask questions every ${shortenedQuestionInterval.toMinutes()} minutes.")
                    appendLine()
                    appendLine(
                        "Teams still seeking: ${
                            game.seekingTeams.filter { !game.foundTeams.contains(it) }.joinToString(", ")
                        }"
                    )
                }
            }
        }
    }

    private suspend fun getCurrentGame(message: Message): Game? {
        val guild = message.getGuild() ?: return null

        // Find the most recent game for this guild
        val game = games.values.filter { it.guild.id == guild.id }.maxByOrNull { it.id }

        if (game == null) {
            message.reply {
                content = "No game has been started. Start a new game with `/hns start`."
            }
            return null
        }

        return game
    }

    private fun extractUserId(mention: String): Snowflake? {
        val regex = Regex("<@!?(\\d+)>")
        val match = regex.find(mention) ?: return null
        return Snowflake(match.groupValues[1])
    }

    class Game(
        val id: Int,
        val guild: Guild,
        private val kord: Kord
    ) {
        val players = mutableMapOf<User, String>() // User to team mapping
        var hidingTeam: String? = null
        val seekingTeams = mutableSetOf<String>()
        val teamChannels = mutableMapOf<String, TextChannelThread>() // Team to channel mapping
        var isActive = false
        var hidingStartTime: Instant? = null
        var seekingStartTime: Instant? = null
        val foundTeams = mutableListOf<String>()
        val foundTimes = mutableMapOf<String, Instant>()
        var round = 1
        private var gameJob: Job? = null
        private var lastHidingTeam: String? = null

        fun addPlayer(user: User, team: String) {
            players[user] = team
        }

        fun getPlayersInTeam(team: String): List<User> {
            return players.filter { it.value == team }.keys.toList()
        }

        fun shuffleTeams() {
            val teams = players.values.toSet().toList()

            // Don't select the same hiding team twice in a row
            val availableTeams = if (lastHidingTeam != null) {
                teams.filter { it != lastHidingTeam }
            } else {
                teams
            }

            hidingTeam = availableTeams.random()
            lastHidingTeam = hidingTeam
            seekingTeams.clear()
            seekingTeams.addAll(teams.filter { it != hidingTeam })
        }

        suspend fun startHidingPhase(message: Message) {
            if (isActive) {
                return
            }

            isActive = true
            hidingStartTime = Instant.now()

            // Create channels for each seeking team
            val hidingTeamPlayers = getPlayersInTeam(hidingTeam!!)

            // Avoid creating roles at all cost as per requirements

            // Create channels for each seeking team
            seekingTeams.forEach { seekingTeam ->
                val seekingTeamPlayers = getPlayersInTeam(seekingTeam)

                // Create a private thread in the channel where the command was executed
                val threadName = "hns-${id}-${round}-$seekingTeam"

                // Create an initial message
                val initialMessageChannel = message.channel.asChannel() as? TextChannel ?: return@forEach

                // Create a thread from the message
                val channel = initialMessageChannel.startPrivateThread(threadName) {
                    reason = "Hiding Team $hidingTeam for game $gameCounter round $round"
                    autoArchiveDuration = ArchiveDuration.Day
                }

                seekingTeamPlayers.forEach { player ->
                    channel.addUser(player.id)
                }
                hidingTeamPlayers.forEach { player ->
                    channel.addUser(player.id)
                }

                teamChannels[seekingTeam] = channel

                // Send initial message to the channel
                channel.createMessage(buildString {
                    appendLine("# Hide and Seek Game #$id round $round")
                    appendLine()
                    appendLine("**Hiding Team: $hidingTeam**")
                    appendLine(hidingTeamPlayers.joinToString(", ") { it.mention })
                    appendLine()
                    appendLine("**Seeking Team: $seekingTeam**")
                    appendLine(seekingTeamPlayers.joinToString(", ") { it.mention })
                    appendLine()
                    appendLine("The hiding phase has started! The hiding team has ${hidingPhaseDuration.toMinutes()} minutes to reach their hiding location.")
                    appendLine("Seeking team, please wait until the hiding phase is over.")
                    appendLine("While you wait, at least one of you must share their location with at least one of the hiders until you start seeking, so the hiders can track where you are to start getting into position when you are close.")
                    appendLine()
                    appendLine("Hiding phase ends in ${hidingPhaseDuration.toMinutes()} minutes")
                })
            }

            // Start the game job to manage phases
            gameJob = CoroutineScope(Dispatchers.Default).launch {
                // Wait for hiding phase to end
                delay(hidingPhaseDuration.toMillis())
                seekingStartTime = Instant.now()

                // Notify all channels that seeking phase has started
                teamChannels.values.forEach { channel ->
                    channel.createMessage(buildString {
                        appendLine("# Seeking Phase Started!")
                        appendLine()
                        appendLine("The hiding phase is over! Seeking teams can now start looking for the hiding team.")
                        appendLine()
                        appendLine("**Rules Summary:**")
                        appendLine("- Hiding team must be within 500m of a Metro station")
                        appendLine("- Seeking teams can ask one question every ${questionInterval.toMinutes()} minutes")
                        appendLine("- Questions must be answered within 10 minutes")
                        appendLine("- When a team finds the hiders, other teams can ask questions every ${shortenedQuestionInterval.toMinutes()} minutes")
                        appendLine("- Game ends when all teams find the hiders")
                        appendLine()
                        appendLine("For full rules and question types, see: https://github.com/mtib/KetchupBot/blob/main/docs/hide_and_seek/HideAndSeek_short.md")
                    })
                }

                // Start question reminder loop
                while (isActive && !allTeamsFound()) {
                    // Wait for the question interval
                    delay(questionInterval.toMillis())

                    // Send question reminders to teams that haven't found the hiders yet
                    teamChannels.forEach { (team, channel) ->
                        if (!foundTeams.contains(team)) {
                            channel.createMessage(
                                """
                                # Question Time!

                                You can now ask another question to the hiding team.
                        
                                When you find the hiding team use `/hns found $team` to stop the timer and announce you won!
                            """.trimIndent()
                            )
                        }
                    }
                }
            }

            message.reply {
                content = buildString {
                    appendLine("# Hide and Seek Game #$id Started")
                    appendLine()
                    appendLine("The hiding phase has started! Team $hidingTeam has ${hidingPhaseDuration.toMinutes()} minutes to reach their hiding location.")
                    appendLine()
                    appendLine("Channels have been created for each seeking team.")
                }
            }
        }

        suspend fun markTeamAsFound(team: String) {
            foundTeams.add(team)
            foundTimes[team] = Instant.now()

            // Notify the team's channel
            teamChannels[team]?.createMessage(buildString {
                appendLine("# Congratulations!")
                appendLine()
                appendLine("Team $team has found the hiding team!")
                val duration = Duration.between(seekingStartTime, foundTimes[team])
                appendLine("Time taken: $duration")
            })

            // Notify other teams
            teamChannels.forEach { (otherTeam, channel) ->
                if (otherTeam != team && !foundTeams.contains(otherTeam)) {
                    channel.createMessage(buildString {
                        appendLine("# Team $team Found the Hiders")
                        appendLine()
                        appendLine("Team $team has found the hiding team!")
                        appendLine()
                        appendLine("You can now ask questions every ${shortenedQuestionInterval.toMinutes()} minutes.")
                    })
                }
            }

            // If all teams have found the hiders, end the game
            if (allTeamsFound()) {
                endGame()
            } else {
                // Update the question interval for remaining teams
                gameJob?.cancel()
                gameJob = CoroutineScope(Dispatchers.Default).launch {
                    while (isActive && !allTeamsFound()) {
                        // Wait for the shortened question interval
                        delay(shortenedQuestionInterval.toMillis())

                        // Send question reminders to teams that haven't found the hiders yet
                        teamChannels.forEach { (team, channel) ->
                            if (!foundTeams.contains(team)) {
                                channel.createMessage(
                                    """
                                    # Question Time!

                                    You can now ask another question to the hiding team.
                        
                                    When you find the hiding team use `/hns found $team` to stop the timer and announce you won!
                                """.trimIndent()
                                )
                            }
                        }
                    }
                }
            }
        }

        fun allTeamsFound(): Boolean {
            return foundTeams.size == seekingTeams.size
        }

        private suspend fun endGame() {
            isActive = false
            gameJob?.cancel()

            // Send final results to all channels
            teamChannels.values.forEach { channel ->
                channel.createMessage(buildString {
                    appendLine("# Game Over!")
                    appendLine()
                    appendLine("All teams have found the hiding team!")
                    appendLine()
                    appendLine("**Final Rankings:**")
                    foundTeams.forEachIndexed { index, team ->
                        val foundTime = foundTimes[team]
                        val duration = Duration.between(seekingStartTime, foundTime)
                        val minutes = duration.toMinutes()
                        val seconds = duration.seconds % 60
                        appendLine("${index + 1}. Team $team - Found in ${minutes}m ${seconds}s")
                    }
                    appendLine()
                    appendLine("Thank you for playing Hide and Seek!")
                    appendLine("Use `/hns shuffle` and `/hns hide` to play again with new teams.")
                })
            }

            round += 1
            teamChannels.clear()
            foundTeams.clear()
            foundTimes.clear()
            hidingStartTime = null
            seekingStartTime = null
        }

        fun cancel() {
            isActive = false
            gameJob?.cancel()
        }
    }
}
