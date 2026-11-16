package dev.fnc314.aijobtracker.frontend.core.data.platform

/**
 * Web platform cannot access local filesystem directly.
 * Always returns null.
 */
public actual object PlatformFileReader {
    public actual fun readText(path: String): String? {
        return null
    }
}
