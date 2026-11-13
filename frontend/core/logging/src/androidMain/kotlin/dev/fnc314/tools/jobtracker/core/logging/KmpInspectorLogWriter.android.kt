package dev.fnc314.tools.jobtracker.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.waqas028.kmpinspector.InspectorLog

public actual class KmpInspectorLogWriter : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable? = null) {
        when (severity) {
            Severity.Verbose -> InspectorLog.v(tag, message)
            Severity.Debug -> InspectorLog.d(tag, message)
            Severity.Info -> InspectorLog.i(tag, message)
            Severity.Warn -> InspectorLog.w(tag, message)
            Severity.Error -> InspectorLog.e(tag, message)
            Severity.Assert -> InspectorLog.e(tag, message)
        }
        throwable?.let { InspectorLog.e(tag, it.stackTraceToString()) }
    }
}
