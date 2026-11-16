package dev.fnc314.aijobtracker.backend.compilation

import kotlinx.coroutines.flow.Flow

/**
 * Functional interface for compiling document source into formatted PDF artifacts.
 */
public fun interface DocumentCompilerEngine {
    /**
     * Compiles raw document markup (e.g. LaTeX or Typst) into an output artifact.
     *
     * @param sourceContent Raw document source string.
     * @param targetFilename Destination artifact filename (e.g. `main_resume.pdf`).
     * @return Flow emitting [CompilationResult] updates.
     */
    public operator fun invoke(
        sourceContent: String,
        targetFilename: String
    ): Flow<CompilationResult>
}
