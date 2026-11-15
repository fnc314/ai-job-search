package dev.fnc314.tools.jobtracker.core.data.platform

/** Actual platform implementation of [PlatformCapabilities] for iOS. */
public actual object PlatformCapabilities {
    public actual val supportsInAppTextViewerToggle: Boolean = true
}
