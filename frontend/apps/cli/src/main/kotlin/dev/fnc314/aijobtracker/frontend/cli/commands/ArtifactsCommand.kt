package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.google.cloud.Timestamp
import com.google.cloud.firestore.SetOptions
import com.google.firebase.cloud.StorageClient
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/**
 * Command group for artifact registration, uploading, and local directory syncing.
 */
public class ArtifactsCommand : CliktCommand(name = "artifacts") {
    init {
        subcommands(
            ArtifactsAddCommand(),
            ArtifactsSyncCommand(),
        )
    }

    override fun run(): Unit = Unit
}

internal class ArtifactsAddCommand : CliktCommand(name = "add") {
    private val applicationId: String by argument("id", help = "Application ID")
    private val path: String? by option("--path", help = "Local file path")
    private val kind: String? by option("--kind", help = "Artifact type (e.g. artifact:cv, artifact:cover-letter)")
    private val filename: String? by option("--filename", help = "Override stored filename")
    private val noUpload: Boolean by option("--no-upload", help = "Record path only without uploading to bucket").flag(default = false)
    private val dryRun: Boolean by option("--dry-run", help = "Preview without writing").flag(default = false)

    override fun run() {
        val t = Terminal()
        if (path.isNullOrBlank()) {
            t.println(red("Error: --path <file> is required."))
            return
        }

        val file = resolveFile(path!!)
        if (!file.exists()) {
            t.println(red("Error: file not found: ${file.absolutePath}"))
            return
        }

        if (!dryRun) {
            RemoteDataSourceProvider.assertWritesAllowed("artifact registration")
        }

        val db = RemoteDataSourceProvider.remote
        val appRef = db.collection("applications").document(applicationId)
        val snap = appRef.get().get()
        if (!snap.exists()) {
            t.println(red("Application not found: $applicationId"))
            return
        }

        val artifactName = filename ?: file.name
        val artifactKind = kind ?: inferKind(artifactName)
        val sha256 = computeSha256(file)
        val artifactId = UUID.randomUUID().toString()
        val now = Timestamp.now()

        t.println(cyan("Registering artifact for $applicationId: $artifactName ($artifactKind)..."))

        var remoteUrl: String? = null
        if (!noUpload && !dryRun) {
            val bucketName = System.getenv("TRACKER_BUCKET") ?: "dev-fnc314-ai-job-tracker-artifacts"
            try {
                val bucket = StorageClient.getInstance().bucket(bucketName)
                val blobPath = "artifacts/$applicationId/$artifactName"
                val blob = bucket.create(blobPath, file.readBytes(), "application/octet-stream")
                remoteUrl = "gs://$bucketName/$blobPath"
                t.println(green("✓ Uploaded to $remoteUrl"))
            } catch (e: Exception) {
                t.println(yellow("Warning: could not upload to GCS bucket: ${e.message}"))
            }
        }

        val docData = mutableMapOf<String, Any?>(
            "artifactId" to artifactId,
            "filename" to artifactName,
            "kind" to artifactKind,
            "localPath" to file.path,
            "sha256" to sha256,
            "sizeBytes" to file.length(),
            "createdAt" to now,
            "updatedAt" to now
        )
        remoteUrl?.let { docData["remoteUrl"] = it }

        if (dryRun) {
            t.println(green("✓ [dry-run] Validated artifact registration: $docData"))
            return
        }

        appRef.collection("artifacts").document(artifactId).set(docData, SetOptions.merge()).get()
        t.println(green("✓ Successfully registered artifact $artifactId for application $applicationId"))
    }
}

internal class ArtifactsSyncCommand : CliktCommand(name = "sync") {
    private val company: String? by option("--company", help = "Limit sync to a specific company directory")
    private val dryRun: Boolean by option("--dry-run", help = "Preview changes without writing").flag(default = false)

    override fun run() {
        val t = Terminal()
        t.println(cyan("Scanning local job-applications directory for artifacts..."))

        val docsDir = findJobApplicationsDir()
        if (docsDir == null || !docsDir.exists()) {
            t.println(yellow("Directory job-applications/ does not exist. Nothing to sync."))
            return
        }

        if (!dryRun) {
            RemoteDataSourceProvider.assertWritesAllowed("artifacts sync")
        }

        val db = RemoteDataSourceProvider.remote
        val appsSnapshot = db.collection("applications").get().get()
        val allApps = appsSnapshot.documents
        val appById = allApps.associateBy { it.id.lowercase().trim() }
        val appByCompany = allApps.associateBy {
            it.getString("company")?.lowercase()?.trim() ?: it.id
        }

        var foundCount = 0
        var syncedCount = 0
        val targetCompany = company?.lowercase()?.trim()

        val companyDirs = docsDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") } ?: emptyList()
        val now = Timestamp.now()

        for (cDir in companyDirs) {
            if (targetCompany != null && !cDir.name.lowercase().contains(targetCompany)) {
                continue
            }

            // Each company directory has subdirectories per role or date
            val companyName = cDir.name.lowercase().trim()
            val filesUnderCompany = cDir.walkTopDown().filter { file ->
                file.isFile &&
                    !file.name.startsWith(".") &&
                    file.name != "cover.cls" &&
                    !file.path.contains("/OpenFonts/") &&
                    !file.name.endsWith(".aux") &&
                    !file.name.endsWith(".log") &&
                    !file.name.endsWith(".out")
            }.toList()

            for (f in filesUnderCompany) {
                // Determine application ID from relative path or company/role mapping
                val relPath = f.relativeTo(cDir).path
                val pathSegments = relPath.split(File.separatorChar)

                val roleFolder = if (pathSegments.size >= 2) pathSegments[0] else null
                val candidateAppId = if (roleFolder != null) {
                    "${cDir.name}--$roleFolder"
                } else {
                    cDir.name
                }

                val matchedApp = appById[candidateAppId]
                    ?: appById[candidateAppId.replace("_", "-")]
                    ?: allApps.find { doc ->
                        val docComp = (doc.getString("company") ?: "").lowercase().replace("\\s+".toRegex(), "-")
                        val docCompId = (doc.getString("companyId") ?: "").lowercase()
                        val isCompanyMatch = docComp == companyName || docCompId == companyName || doc.id.startsWith("$companyName--")
                        if (!isCompanyMatch) return@find false
                        if (roleFolder != null) {
                            val roleNormalized = roleFolder.replace("-", "").lowercase()
                            val docIdNormalized = doc.id.replace("-", "").lowercase()
                            val docRoleNormalized = (doc.getString("role") ?: "").replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
                            docIdNormalized.contains(roleNormalized) || (docRoleNormalized.isNotEmpty() && roleNormalized.contains(docRoleNormalized))
                        } else {
                            true
                        }
                    }

                val appId = matchedApp?.id ?: candidateAppId
                foundCount++

                val artifactName = f.name
                val artifactKind = inferKind(artifactName)
                val sha256 = computeSha256(f)

                if (dryRun) {
                    t.println("  • Discovered: [${cDir.name}] $relPath ($artifactKind) -> appId: $appId")
                } else {
                    val appRef = db.collection("applications").document(appId)
                    val existingSnap = appRef.collection("artifacts")
                        .whereEqualTo("filename", artifactName)
                        .get().get()

                    val artifactId = if (!existingSnap.isEmpty) {
                        existingSnap.documents[0].id
                    } else {
                        UUID.randomUUID().toString()
                    }

                    val docData = mutableMapOf<String, Any?>(
                        "artifactId" to artifactId,
                        "filename" to artifactName,
                        "kind" to artifactKind,
                        "localPath" to f.absolutePath,
                        "sha256" to sha256,
                        "sizeBytes" to f.length(),
                        "updatedAt" to now
                    )
                    if (existingSnap.isEmpty) {
                        docData["createdAt"] = now
                    }

                    appRef.collection("artifacts").document(artifactId).set(docData, SetOptions.merge()).get()
                    syncedCount++
                }
            }
        }

        if (dryRun) {
            t.println(green("✓ [dry-run] Discovered $foundCount local artifact file(s) across ${companyDirs.size} company directories."))
        } else {
            t.println(green("✓ Successfully synced $syncedCount / $foundCount artifact file(s) across ${companyDirs.size} company directories."))
        }
    }
}

private fun findJobApplicationsDir(): File? {
    val envRoot = System.getenv("MISE_PROJECT_ROOT")
    if (!envRoot.isNullOrBlank()) {
        val cand = File(envRoot, "job-applications")
        if (cand.exists()) return cand
    }
    var current: File? = File(".").canonicalFile
    while (current != null) {
        val cand = File(current, "job-applications")
        if (cand.exists()) return cand
        current = current.parentFile
    }
    return null
}

private fun resolveFile(path: String): File {
    val f = File(path)
    if (f.isAbsolute || f.exists()) return f
    val envRoot = System.getenv("MISE_PROJECT_ROOT")
    if (!envRoot.isNullOrBlank()) {
        val cand = File(envRoot, path)
        if (cand.exists()) return cand
    }
    var current: File? = File(".").canonicalFile
    while (current != null) {
        val cand = File(current, path)
        if (cand.exists()) return cand
        current = current.parentFile
    }
    return f
}

private fun inferKind(filename: String): String {
    val lower = filename.lowercase()
    return when {
        lower.startsWith("main") || lower.contains("cv") || lower.contains("resume") -> "artifact:cv"
        lower.startsWith("cover") || lower.contains("letter") -> "artifact:cover-letter"
        lower.contains("jd") || lower.contains("description") -> "artifact:job-description"
        lower.endsWith(".md") -> "artifact:outcome"
        else -> "artifact:other"
    }
}

private fun computeSha256(file: File): String {
    val md = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { fis ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}
