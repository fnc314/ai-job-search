package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

public class BackfillParentCommand : CliktCommand(name = "backfill") {
    init {
        subcommands(BackfillEmailsCommand())
    }

    override fun run() = Unit
}
