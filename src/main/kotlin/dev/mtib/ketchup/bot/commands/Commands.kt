package dev.mtib.ketchup.bot.commands

import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module as koinModule


class Commands {
    companion object {
        val module = koinModule {
            single { HelpCommand(get()) } bind Command::class
            single { PingCommand(get()) } bind Command::class
            single { AboutCommand() } bind Command::class
            single { ArchiveEventsCommand() } binds arrayOf(Command::class, AdminCommand::class)
            single { CreateEventCommand(get()) } bind Command::class
            single { QuittingCommand() } bind Command::class
            single { CreateClubCommand(get()) } bind Command::class
            single { RoadmapCommand() } bind Command::class
        }
    }
}