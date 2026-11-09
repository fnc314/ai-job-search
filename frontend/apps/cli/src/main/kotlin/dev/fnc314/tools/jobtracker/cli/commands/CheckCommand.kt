package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider

public class CheckCommand : CliktCommand(name = "check") {
    override fun run() {
        val t = Terminal()
        try {
            val db = FirestoreProvider.firestore
            val count = db.collection("applications").limit(1).get().get().size()
            t.println(green("✓ Connection to Firestore verified successfully (applications accessible, count >= $count)"))
        } catch (e: Exception) {
            t.println(red("✗ Firestore connection check failed: ${e.message}"))
            throw e
        }
    }
}
