package dev.fnc314.tools.jobtracker.app.core

import co.touchlab.kermit.LogWriter

public actual class KmpInspectorLogWriter : LogWriter() {
    // No-op: kmp-inspector does not support web targets
}
