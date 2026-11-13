package dev.fnc314.tools.jobtracker.core.logging

/**
 * Extracts the simple class name of the caller from the stack trace (Android).
 */
internal actual fun getCallerClassName(): String {
    // Stack trace: [0] = getStackTrace, [1] = callerClassName, [2] = defaultTag, [3] = caller
    val stackTrace = Throwable().stackTrace
    val element = stackTrace.getOrNull(3)
    if (element != null) {
        val className = element.className
        return className.substringAfterLast('.')
    }
    return "AppLogger"
}
