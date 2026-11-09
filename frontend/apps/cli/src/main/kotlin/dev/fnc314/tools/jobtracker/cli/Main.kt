package dev.fnc314.tools.jobtracker.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.terminal.Terminal

class JobSearchCli : CliktCommand(name = "jobsearch") {
    override fun run() = Unit
}

class TrackerCommand : CliktCommand(name = "tracker") {
    override fun run() = Unit
}

class ListApplications : CliktCommand(name = "list") {
    override fun run() {
        val t = Terminal()
        t.println(cyan("Loaded job applications via unified Kotlin CLI"))
    }
}

fun main(args: Array<String>) {
    JobSearchCli()
        .subcommands(
            TrackerCommand().subcommands(ListApplications())
        )
        .main(args)
}
