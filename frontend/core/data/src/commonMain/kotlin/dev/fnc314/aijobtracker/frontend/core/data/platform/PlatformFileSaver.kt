package dev.fnc314.aijobtracker.frontend.core.data.platform

/**
 * Platform-specific file saver for writing text content to device storage.
 */
public expect object PlatformFileSaver {
    /**
     * Saves text content to a file on the device.
     *
     * @param content The text content to save.
     * @param filename The desired filename (e.g., "cover-letter.md").
     * @return `Result.success` if the file was saved, `Result.failure` otherwise.
     */
    public suspend fun saveText(content: String, filename: String): Result<Unit>
}
