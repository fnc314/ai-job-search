package dev.fnc314.tools.jobtracker.core.logging

/**
 * Extracts the simple class name of the caller from the stack trace (JS).
 * JS stack traces are not reliably parseable, so we fall back to a default tag.
 */
internal actual fun getCallerClassName(): String {
    // Stack traces are not reliably available in Kotlin/JS
    return "AppLogger"
}
