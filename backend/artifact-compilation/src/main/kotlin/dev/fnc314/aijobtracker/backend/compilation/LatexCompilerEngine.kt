package dev.fnc314.aijobtracker.backend.compilation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Executes system `pdflatex` or `xelatex` commands in a sandboxed working directory.
 */
public class LatexCompilerEngine(
    private val compilerExecutable: String = "pdflatex",
    private val timeoutSeconds: Long = 30
) : DocumentCompilerEngine {

    override fun invoke(
        sourceContent: String,
        targetFilename: String
    ): Flow<CompilationResult> = flow {
        val tempDir = File.createTempFile("latex_build_", "").apply {
            delete()
            mkdirs()
        }

        try {
            val texFile = File(tempDir, "document.tex")
            texFile.writeText(sourceContent)

            val process = ProcessBuilder(
                compilerExecutable,
                "-interaction=nonstopmode",
                "-halt-on-error",
                texFile.name
            )
                .directory(tempDir)
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            val logOutput = process.inputStream.bufferedReader().use { it.readText() }

            if (!completed) {
                process.destroyForcibly()
                emit(
                    CompilationResult(
                        success = false,
                        artifactName = targetFilename,
                        log = "Compilation timed out after $timeoutSeconds seconds.\n$logOutput"
                    )
                )
                return@flow
            }

            val exitCode = process.exitValue()
            val pdfFile = File(tempDir, "document.pdf")

            if (exitCode == 0 && pdfFile.exists()) {
                emit(
                    CompilationResult(
                        success = true,
                        artifactName = targetFilename,
                        artifactUrl = pdfFile.absolutePath,
                        log = logOutput
                    )
                )
            } else {
                emit(
                    CompilationResult(
                        success = false,
                        artifactName = targetFilename,
                        log = "Compilation failed with exit code $exitCode.\n$logOutput"
                    )
                )
            }
        } catch (e: Exception) {
            emit(
                CompilationResult(
                    success = false,
                    artifactName = targetFilename,
                    log = "Error launching $compilerExecutable: ${e.message}"
                )
            )
        } finally {
            // Keep cleanup safe
            tempDir.deleteOnExit()
        }
    }.flowOn(Dispatchers.IO)
}
