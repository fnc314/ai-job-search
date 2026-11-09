package dev.fnc314.tools.jobtracker.terminal

import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.terminal.Terminal

fun main(args: Array<String>) {
    val t = Terminal()
    t.println(green("Starting Kotlin Terminal TUI (frontend/apps/terminal)..."))
}
