package dev.fnc314.aijobtracker.backend.compilation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Service orchestrating remote document compilation and cloud artifact generation.
 */
public class DocumentCompilerService(
    private val compilerEngine: DocumentCompilerEngine? = null
) {
    /**
     * Compiles LaTeX source text into a PDF artifact.
     *
     * @param latexSource Raw LaTeX markup string.
     * @param outputFileName Target artifact name.
     * @return Flow containing the [CompilationResult].
     */
    public fun compileLatex(
        latexSource: String,
        outputFileName: String
    ): Flow<CompilationResult> {
        val engine = compilerEngine ?: LatexCompilerEngine()
        return engine(latexSource, outputFileName)
    }
}
