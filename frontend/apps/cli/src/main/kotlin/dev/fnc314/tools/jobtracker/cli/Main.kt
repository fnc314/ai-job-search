package dev.fnc314.tools.jobtracker.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import dev.fnc314.tools.jobtracker.cli.commands.AddApplicationCommand
import dev.fnc314.tools.jobtracker.cli.commands.AddEventCommand
import dev.fnc314.tools.jobtracker.cli.commands.AlignCommand
import dev.fnc314.tools.jobtracker.cli.commands.BackfillParentCommand
import dev.fnc314.tools.jobtracker.cli.commands.CheckCommand
import dev.fnc314.tools.jobtracker.cli.commands.EditCommand
import dev.fnc314.tools.jobtracker.cli.commands.GetApplicationCommand
import dev.fnc314.tools.jobtracker.cli.commands.MergeCommand
import dev.fnc314.tools.jobtracker.cli.commands.ReclassifyCommand
import dev.fnc314.tools.jobtracker.cli.commands.ListApplicationsCommand
import dev.fnc314.tools.jobtracker.cli.commands.RankCommand
import dev.fnc314.tools.jobtracker.cli.commands.SeedCommand
import dev.fnc314.tools.jobtracker.cli.commands.UpdateScoreCommand
import dev.fnc314.tools.jobtracker.cli.commands.UpdateStatusCommand

public class JobSearchCli : CliktCommand(name = "jobsearch") {
    override fun run(): Unit = Unit
}

public class TrackerCommand : CliktCommand(name = "tracker") {
    init {
        subcommands(
            CheckCommand(),
            ListApplicationsCommand(),
            GetApplicationCommand(),
            AddApplicationCommand(),
            EditCommand(),
            MergeCommand(),
            ReclassifyCommand(),
            UpdateStatusCommand(),
            UpdateScoreCommand(),
            AddEventCommand(),
            BackfillParentCommand(),
            AlignCommand(),
            RankCommand(),
            SeedCommand(),
        )
    }

    override fun run(): Unit = Unit
}

public fun main(args: Array<String>) {
    JobSearchCli()
        .subcommands(TrackerCommand())
        .main(args)
}
