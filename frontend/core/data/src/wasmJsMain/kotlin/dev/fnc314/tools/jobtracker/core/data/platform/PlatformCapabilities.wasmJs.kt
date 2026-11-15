package dev.fnc314.tools.jobtracker.core.data.platform

/** Actual platform implementation of [PlatformCapabilities] for WebAssembly (wasmJs). */
public actual object PlatformCapabilities {
    public actual val supportsInAppTextViewerToggle: Boolean = false
}
