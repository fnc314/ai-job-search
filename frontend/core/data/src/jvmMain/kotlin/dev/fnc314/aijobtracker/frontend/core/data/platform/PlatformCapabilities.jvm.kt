package dev.fnc314.aijobtracker.frontend.core.data.platform

/** Actual platform implementation of [PlatformCapabilities] for JVM / Desktop. */
public actual object PlatformCapabilities {
    public actual val supportsInAppTextViewerToggle: Boolean = false
    public actual val supportsPushNotifications: Boolean = false
}
