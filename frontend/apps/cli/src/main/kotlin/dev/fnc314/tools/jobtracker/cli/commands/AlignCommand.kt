package dev.fnc314.tools.jobtracker.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import dev.fnc314.tools.jobtracker.cli.db.FirestoreProvider

public class AlignCommand : CliktCommand(name = "align") {
    private val dryRun: Boolean by option("--dry-run", help = "Report only without writing changes").flag(default = false)

    override fun run() {
        val t = Terminal()
        t.println(cyan("=== Aligning Job Application Records ==="))

        if (!dryRun) {
            FirestoreProvider.assertWritesAllowed("application alignment")
        } else {
            t.println(yellow("[DRY RUN MODE] Changes will not be persisted to Firestore."))
        }

        val db = FirestoreProvider.firestore
        val appsSnap = db.collection("applications").get().get()
        var alignedCount = 0

        for (appDoc in appsSnap.documents) {
            val data = appDoc.data
            var modified = false
            val updates = mutableMapOf<String, Any>()

            if (!data.containsKey("emailMessageIds")) {
                updates["emailMessageIds"] = emptyList<String>()
                modified = true
            }

            if (!data.containsKey("schemaVersion")) {
                updates["schemaVersion"] = 9
                modified = true
            }

            if (modified) {
                alignedCount++
                if (!dryRun) {
                    updates["updatedAt"] = Timestamp.now()
                    appDoc.reference.update(updates).get()
                }
            }
        }

        t.println(green("✓ Aligned $alignedCount / ${appsSnap.size()} application records."))
    }
}
