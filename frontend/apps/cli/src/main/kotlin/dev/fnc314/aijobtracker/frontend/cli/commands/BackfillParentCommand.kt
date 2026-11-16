package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

internal class BackfillParentCommand : CliktCommand(name = "backfill") {
    init {
        subcommands(BackfillEmailsCommand())
    }

    override fun run() = Unit
}
