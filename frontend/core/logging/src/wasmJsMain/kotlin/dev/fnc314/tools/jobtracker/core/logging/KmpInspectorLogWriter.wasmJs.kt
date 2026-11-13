package dev.fnc314.tools.jobtracker.core.logging

import co.touchlab.kermit.Severity

public actual class KmpInspectorLogWriter : co.touchlab.kermit.LogWriter() {
    // No-op: kmp-inspector does not support web targets
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        // No-op
    }
}
