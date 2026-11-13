package dev.fnc314.tools.jobtracker.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger

/**
 * Application-wide logger that wraps Kermit.
 *
 * Call [init] once at app startup to configure log writers.
 * After initialization, use [v], [d], [i], [w], and [e] to log messages.
 *
 * Tag derivation: if [tag] is not provided, the caller's class name is derived
 * from the stack trace. Supply an explicit tag to avoid the stack inspection cost.
 */
public object AppLogger {

    /**
     * Initialize the global Kermit logger with the provided [writers].
     * Call this once at application startup.
     */
    public fun init(vararg writers: LogWriter) {
        Logger.setLogWriters(writers.toList())
    }

    /** Log a verbose message. */
    public fun v(tag: String = defaultTag(), msg: () -> String) {
        Logger.v(tag = tag, message = msg)
    }

    /** Log a debug message. */
    public fun d(tag: String = defaultTag(), msg: () -> String) {
        Logger.d(tag = tag, message = msg)
    }

    /** Log an info message. */
    public fun i(tag: String = defaultTag(), msg: () -> String) {
        Logger.i(tag = tag, message = msg)
    }

    /** Log a warning message. */
    public fun w(tag: String = defaultTag(), msg: () -> String) {
        Logger.w(tag = tag, message = msg)
    }

    /** Log an error message. */
    public fun e(tag: String = defaultTag(), msg: () -> String) {
        Logger.e(tag = tag, message = msg)
    }

    /** Log an error message with a [throwable] cause. */
    public fun e(tag: String = defaultTag(), throwable: Throwable, msg: () -> String) {
        Logger.e(throwable = throwable, tag = tag, message = msg)
    }

    private fun defaultTag(): String {
        // Stack trace: [0] = getStackTrace, [1] = defaultTag, [2] = caller
        val stackTrace = Throwable().stackTrace
        val className = stackTrace.getOrNull(2)?.className ?: "AppLogger"
        return className.substringAfterLast('.')
    }
}
