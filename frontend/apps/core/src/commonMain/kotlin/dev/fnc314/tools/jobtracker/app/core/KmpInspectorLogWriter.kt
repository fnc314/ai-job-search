package dev.fnc314.tools.jobtracker.app.core

import co.touchlab.kermit.LogWriter

/**
 * Kermit LogWriter that routes log entries to KMP Inspector's Logs panel.
 * No-op on platforms where KMP Inspector is not available (js, wasmJs).
 */
public expect class KmpInspectorLogWriter() : LogWriter
