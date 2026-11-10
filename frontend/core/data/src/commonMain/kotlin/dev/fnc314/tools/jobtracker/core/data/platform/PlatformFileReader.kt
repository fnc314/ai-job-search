package dev.fnc314.tools.jobtracker.core.data.platform

/**
 * Multiplatform reader for local text-based files (Markdown, plain text, etc.).
 *
 * Returns the file content as a String, or `null` if the file cannot be read
 * (e.g., file not found, unsupported platform, or binary content).
 */
public expect object PlatformFileReader {
    /**
     * Reads the content of a local text file.
     *
     * @param path Absolute or relative local filesystem path to the file.
     * @return The file content as a String, or `null` if reading failed.
     */
    public fun readText(path: String): String?
}
