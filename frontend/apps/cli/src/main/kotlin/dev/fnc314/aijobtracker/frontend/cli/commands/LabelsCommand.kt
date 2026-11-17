package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider

internal class LabelsCommand : CliktCommand(name = "labels") {
    override fun run() {
        val t = Terminal()
        val db = RemoteDataSourceProvider.remote
        val snap = db.collection("meta").document("sync").get().get()

        if (!snap.exists()) {
            t.println(yellow("No sync metadata found."))
            return
        }

        val data = snap.data ?: emptyMap<String, Any>()
        val gmailData = data["gmail"] as? Map<*, *> ?: emptyMap<String, Any>()
        val labels = gmailData["labels"] as? List<*> ?: emptyList<Any>()

        t.println(green("Found ${labels.size} label(s):"))
        for (label in labels) {
            t.println("  • $label")
        }
    }
}
