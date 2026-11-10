package dev.fnc314.tools.jobtracker.core.data.platform

/**
 * Multiplatform capability inspector declaring device- and OS-specific feature flags.
 */
public expect object PlatformCapabilities {
    /**
     * Whether this platform supports the user-configurable In-App Text/Markdown viewer setting.
     *
     * True strictly on mobile environments (Android, iOS) where external viewers may be absent.
     * False on Desktop (JVM) and Web, which always delegate to OS-configured editors or browser handlers.
     */
    public val supportsInAppTextViewerToggle: Boolean
}
