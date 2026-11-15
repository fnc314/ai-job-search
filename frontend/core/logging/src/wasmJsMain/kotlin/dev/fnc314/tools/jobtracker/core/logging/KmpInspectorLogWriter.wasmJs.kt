package dev.fnc314.tools.jobtracker.core.logging

import co.touchlab.kermit.Severity
import co.touchlab.kermit.LogWriter

/** Actual platform implementation of [KmpInspectorLogWriter]. */
public actual class KmpInspectorLogWriter : LogWriter() {
    // No-op: kmp-inspector does not support web targets
    actual override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        // No-op
    }
}
