package dev.fnc314.aijobtracker.frontend.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

/**
 * Kermit LogWriter that routes log entries to KMP Inspector's Logs panel.
 * No-op on platforms where KMP Inspector is not available (js, wasmJs).
 */
public expect class KmpInspectorLogWriter() : LogWriter {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?)
}
