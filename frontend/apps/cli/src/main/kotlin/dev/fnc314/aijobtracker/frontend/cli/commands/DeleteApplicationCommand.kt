package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider

internal class DeleteApplicationCommand : CliktCommand(name = "delete") {
    private val ids: List<String> by argument(
        name = "ids",
        help = "Application IDs (or unique prefixes) to delete",
    ).multiple(required = true)
    private val dryRun: Boolean by option(
        names = arrayOf("--dry-run"),
        help = "Preview deletions without modifying data",
    ).flag()

    override fun run() {
        val terminal = Terminal()
        if (!dryRun) {
            RemoteDataSourceProvider.assertWritesAllowed("delete applications")
        }

        val remote = RemoteDataSourceProvider.remote
        var deletedTargetCompanyApp = false

        for (idOrPrefix in ids) {
            val resolved = resolveApplication(remote, idOrPrefix, terminal) ?: continue
            val isTargetCompany = isTargetCompanyRecord(resolved)
            deleteSingleApplication(remote, resolved, terminal)
            if (isTargetCompany) {
                deletedTargetCompanyApp = true
            }
        }

        if (deletedTargetCompanyApp) {
            cleanupTargetCompanyIfUnused(remote, terminal)
        }
    }

    private fun resolveApplication(
        remote: Firestore,
        idOrPrefix: String,
        terminal: Terminal,
    ): DocumentSnapshot? {
        val appsCollection = remote.collection("applications")
        val exactSnap = appsCollection.document(idOrPrefix).get().get()
        if (exactSnap.exists()) {
            return exactSnap
        }

        val matches = appsCollection.get().get().documents.filter { it.id.startsWith(idOrPrefix) }
        return when {
            matches.isEmpty() -> {
                terminal.println(red("Application not found: $idOrPrefix"))
                null
            }
            matches.size > 1 -> {
                val matchedIds = matches.joinToString(", ") { it.id }
                terminal.println(red("Prefix '$idOrPrefix' is ambiguous ($matchedIds)"))
                null
            }
            else -> matches.first()
        }
    }

    private fun deleteSingleApplication(
        remote: Firestore,
        docSnap: DocumentSnapshot,
        terminal: Terminal,
    ) {
        val resolvedId = docSnap.id
        val company = docSnap.getString("company").orEmpty()
        val role = docSnap.getString("role").orEmpty()
        val docRef = docSnap.reference

        val eventDocs = docRef.collection("events").get().get().documents
        val artifactDocs = docRef.collection("artifacts").get().get().documents
        val artifactJobRefs = findArtifactJobRefs(remote, resolvedId)

        if (dryRun) {
            terminal.println(
                yellow(
                    "[dry-run] Would delete '$resolvedId' ($company / $role): " +
                        "${eventDocs.size} event(s), ${artifactDocs.size} artifact(s), " +
                        "${artifactJobRefs.size} artifact_job(s)",
                ),
            )
            return
        }

        eventDocs.forEach { it.reference.delete().get() }
        artifactDocs.forEach { it.reference.delete().get() }
        artifactJobRefs.forEach { it.delete().get() }
        docRef.delete().get()

        terminal.println(
            green(
                "✓ Deleted '$resolvedId' ($company / $role) " +
                    "[${eventDocs.size} events, ${artifactDocs.size} artifacts, " +
                    "${artifactJobRefs.size} artifact_jobs]",
            ),
        )
    }

    private fun findArtifactJobRefs(
        remote: Firestore,
        resolvedId: String,
    ): List<com.google.cloud.firestore.DocumentReference> {
        val jobsCollection = remote.collection("artifact_jobs")
        val byAppId = jobsCollection.whereEqualTo("applicationId", resolvedId).get().get().documents
            .map { it.reference }
            .associateBy { it.path }
            .toMutableMap()

        if (resolvedId.startsWith("apply-")) {
            val candidateJobId = resolvedId.replaceFirst("apply-", "job-")
            val jobSnap = jobsCollection.document(candidateJobId).get().get()
            if (jobSnap.exists()) {
                byAppId[jobSnap.reference.path] = jobSnap.reference
            }
        }
        return byAppId.values.toList()
    }

    private fun isTargetCompanyRecord(docSnap: DocumentSnapshot): Boolean {
        val companyId = docSnap.getString("companyId").orEmpty()
        val company = docSnap.getString("company").orEmpty()
        return companyId == "target-company" || company.equals("Target Company", ignoreCase = true)
    }

    private fun cleanupTargetCompanyIfUnused(
        remote: Firestore,
        terminal: Terminal,
    ) {
        val targetCompanyRef = remote.collection("companies").document("target-company")
        val targetCompanySnap = targetCompanyRef.get().get()
        if (!targetCompanySnap.exists()) {
            return
        }

        val appsCollection = remote.collection("applications")
        val remainingBySlug = appsCollection.whereEqualTo("companyId", "target-company").get().get().documents
        val remainingByName = appsCollection.whereEqualTo("company", "Target Company").get().get().documents
        if (remainingBySlug.isNotEmpty() || remainingByName.isNotEmpty()) {
            terminal.println(yellow("Kept companies/target-company because remaining applications reference it."))
            return
        }

        if (dryRun) {
            terminal.println(yellow("[dry-run] Would delete companies/target-company"))
            return
        }

        targetCompanyRef.delete().get()
        terminal.println(green("✓ Deleted orphan companies/target-company"))
    }
}
