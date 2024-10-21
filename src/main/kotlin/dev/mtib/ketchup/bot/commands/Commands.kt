package dev.mtib.ketchup.bot.commands

import dev.mtib.ketchup.bot.commands.ai.DalleCommand
import dev.mtib.ketchup.bot.commands.ai.GptCommand
import dev.mtib.ketchup.bot.commands.schedule.ScheduleMessageCommand
import dev.mtib.ketchup.bot.commands.schedule.ScheduledMessageStatsCommand
import dev.mtib.ketchup.bot.commands.subscriptions.*
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module as koinModule


class Commands {
    companion object {
        val module = koinModule {
            single { DiceCommand() } bind Command::class
            single { HelpCommand } bind Command::class
            single { LeaveCommand() } bind Command::class
            single { PingCommand() } bind Command::class
            single { RoadmapCommand() } bind Command::class
            single { StatsCommand() } bind Command::class
            single { KetchupRankCommand() } bind Command::class
            single { GptCommand() } bind Command::class
            single { DalleCommand() } bind Command::class
            single { SubscriptionCreateCommand() } bind Command::class
            single { ListSubscriptionsCommand() } bind Command::class
            single { SubscribeCommand() } bind Command::class
            single { UnsubscribeCommand() } bind Command::class
            single { PostSubscriberMessageCommand() } bind Command::class
            single { IterateMembersCommand() } bind Command::class
            single { ScheduleMessageCommand } bind Command::class
            single { ScheduledMessageStatsCommand } binds arrayOf(Command::class, AdminCommand::class)
            single { ReactionSubscriptionCommand } bind Command::class
        }
    }
}