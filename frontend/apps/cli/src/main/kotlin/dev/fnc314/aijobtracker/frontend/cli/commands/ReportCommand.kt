package dev.fnc314.aijobtracker.frontend.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.terminal.Terminal
import dev.fnc314.aijobtracker.frontend.cli.db.RemoteDataSourceProvider
import java.io.File
import java.time.Instant

private fun findProjectRoot(): File {
    var dir = File(".").canonicalFile
    while (dir.parentFile != null && !File(dir, "mise.toml").exists()) {
        dir = dir.parentFile
    }
    return dir
}

/**
 * Command to generate the HTML applications dashboard report snapshot.
 */
public class ReportCommand : CliktCommand(name = "report") {
    private val output: String? by option("-o", "--output", help = "Output path for the HTML dashboard (default: ui/web/application-dashboard.html)")

    override fun run() {
        val t = Terminal()
        val destPath = output ?: File(findProjectRoot(), "ui/web/application-dashboard.html").path
        val destFile = File(destPath)
        destFile.parentFile?.mkdirs()

        t.println(cyan("Generating application report snapshot from Firestore..."))

        val db = RemoteDataSourceProvider.remote
        val apps = db.collection("applications").get().get().documents.map { it.data }

        val html = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("  <meta charset=\"utf-8\">")
            appendLine("  <title>Application Dashboard Report</title>")
            appendLine("  <style>")
            appendLine("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 2rem; background: #f8fafc; color: #1e293b; }")
            appendLine("    h1 { color: #0f172a; }")
            appendLine("    .meta { font-size: 0.875rem; color: #64748b; margin-bottom: 1.5rem; }")
            appendLine("    table { width: 100%; border-collapse: collapse; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }")
            appendLine("    th, td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e2e8f0; }")
            appendLine("    th { background: #f1f5f9; font-weight: 600; font-size: 0.875rem; color: #475569; }")
            appendLine("    tr:hover { background: #f8fafc; }")
            appendLine("    .badge { display: inline-block; padding: 0.25rem 0.5rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; }")
            appendLine("    .badge-APPLIED { background: #e0f2fe; color: #0369a1; }")
            appendLine("    .badge-INTERVIEWING { background: #fef3c7; color: #b45309; }")
            appendLine("    .badge-REJECTED { background: #fee2e2; color: #b91c1c; }")
            appendLine("    .badge-OFFER { background: #dcfce7; color: #15803d; }")
            appendLine("  </style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("  <h1>Application Dashboard Report</h1>")
            appendLine("  <div class=\"meta\">Generated: ${Instant.now()} | Applications: ${apps.size}</div>")
            appendLine("  <table>")
            appendLine("    <thead>")
            appendLine("      <tr><th>Company</th><th>Role</th><th>Status</th><th>Date</th><th>Fit Score</th></tr>")
            appendLine("    </thead>")
            appendLine("    <tbody>")
            for (app in apps) {
                val company = app["company"]?.toString() ?: "-"
                val role = app["role"]?.toString() ?: "-"
                val status = app["status"]?.toString() ?: "UNKNOWN"
                val date = (app["appliedDate"] ?: app["date"] ?: app["updatedAt"])?.toString()?.take(10) ?: "-"
                val fit = app["fitScore"]?.toString() ?: "-"
                appendLine("      <tr>")
                appendLine("        <td><strong>$company</strong></td>")
                appendLine("        <td>$role</td>")
                appendLine("        <td><span class=\"badge badge-$status\">$status</span></td>")
                appendLine("        <td>$date</td>")
                appendLine("        <td>$fit</td>")
                appendLine("      </tr>")
            }
            appendLine("    </tbody>")
            appendLine("  </table>")
            appendLine("</body>")
            appendLine("</html>")
        }

        destFile.writeText(html)
        t.println(green("✓ Successfully generated HTML dashboard report (${apps.size} applications) at ${destFile.path}"))
    }
}
