package dev.fnc314.aijobtracker.backend.compilation

import kotlinx.serialization.Serializable

/**
 * Result model emitted upon completing document compilation.
 *
 * @property success True if document compilation succeeded with zero fatal errors.
 * @property artifactName Output filename of the compiled document.
 * @property artifactUrl Public or storage URL where the compiled PDF is hosted.
 * @property log Output build log, warnings, or compilation errors.
 */
@Serializable
public data class CompilationResult(
    val success: Boolean,
    val artifactName: String,
    val artifactUrl: String? = null,
    val log: String = ""
)
