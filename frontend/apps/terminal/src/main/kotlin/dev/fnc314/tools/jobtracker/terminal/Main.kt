package dev.fnc314.tools.jobtracker.terminal

import dev.fnc314.tools.jobtracker.terminal.tracker.TerminalApp

/**
 * Entry point for the terminal application.
 * Dispatches to the appropriate sub-app based on command-line arguments:
 * - `--roadmap` → RoadmapApp (roadmap TUI)
 * - `--proposals` → TerminalApp proposals mode
 * - `--emails` → EmailsApp (email rectification TUI)
 * - default → TerminalApp (job tracker TUI)
 */
public fun main(args: Array<String>) {
    TerminalApp.run(args)
}
