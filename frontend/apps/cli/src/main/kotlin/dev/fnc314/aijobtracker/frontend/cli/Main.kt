package dev.fnc314.aijobtracker.frontend.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import dev.fnc314.aijobtracker.frontend.cli.commands.AddApplicationCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.AddEventCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.AlignCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.ApplyCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.BackfillParentCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.ArtifactsCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.BulkCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.CheckCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.DeleteApplicationCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.EditCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.EmailCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.ExportCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.GetApplicationCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.ImportCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.LabelsCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.LeadCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.MergeCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.ReclassifyCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.ReportCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.ListApplicationsCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.ProposalCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.RankCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.SalaryCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.SeedCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.UpdateScoreCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.UpdateStatusCommand
import dev.fnc314.aijobtracker.frontend.cli.commands.UpsertCommand

internal class JobSearchCli : CliktCommand(name = "jobsearch") {
    override fun run(): Unit = Unit
}

internal class TrackerCommand : CliktCommand(name = "tracker") {
    init {
        subcommands(
            CheckCommand(),
            ListApplicationsCommand(),
            GetApplicationCommand(),
            AddApplicationCommand(),
            DeleteApplicationCommand(),
            EditCommand(),
            ExportCommand(),
            ImportCommand(),
            ReportCommand(),
            LeadCommand(),
            MergeCommand(),
            ReclassifyCommand(),
            UpdateStatusCommand(),
            UpdateScoreCommand(),
            AddEventCommand(),
            BackfillParentCommand(),
            AlignCommand(),
            ApplyCommand(),
            RankCommand(),
            SalaryCommand(),
            SeedCommand(),
            ProposalCommand(),
            LabelsCommand(),
            UpsertCommand(),
            BulkCommand(),
        )
    }

    override fun run(): Unit = Unit
}

/**
 * Entry point for the job search command-line interface.
 */
public fun main(args: Array<String>) {
    JobSearchCli()
        .subcommands(TrackerCommand(), ApplyCommand(), ProposalCommand(), SalaryCommand(), EmailCommand(), ArtifactsCommand(), ReportCommand())
        .main(args)
}
