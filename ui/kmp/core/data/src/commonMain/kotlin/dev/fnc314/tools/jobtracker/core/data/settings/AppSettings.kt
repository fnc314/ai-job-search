package dev.fnc314.tools.jobtracker.core.data.settings

import kotlinx.serialization.Serializable

/**
 * Display theme modes configuring light, dark, or system-following appearances.
 */
@Serializable
public enum class UiMode {
    /** Follows the host operating system's light or dark mode setting. */
    SYSTEM,

    /** Forces light mode theme across all screens. */
    LIGHT,

    /** Forces dark mode theme across all screens. */
    DARK
}

/**
 * User application preferences governing visual themes, column visibility, and rendering options.
 *
 * @property showDate Whether to display submission and activity dates in list rows.
 * @property showCompany Whether to display company names in compact list rows.
 * @property showStatus Whether to render the status badge in list cards.
 * @property showFit Whether to render the fit rating badge in list cards.
 * @property showRole Whether to display the role title in list cards.
 * @property uiMode Selected theme appearance mode [UiMode]. Defaults to [UiMode.SYSTEM].
 * @property dynamicColor Whether to enable Android 12+ wallpaper dynamic color extraction.
 * @property seedColorHex Canonical theme seed color hexadecimal string (default: `"#2F5597"`).
 * @property renderMarkdownInternally Whether to render Markdown notes inline inside the UI.
 * @see UiMode
 */
@Serializable
public data class AppSettings(
    public val showDate: Boolean = true,
    public val showCompany: Boolean = true,
    public val showStatus: Boolean = true,
    public val showFit: Boolean = true,
    public val showRole: Boolean = true,
    public val uiMode: UiMode = UiMode.SYSTEM,
    public val dynamicColor: Boolean = false,
    public val seedColorHex: String = "#2F5597",
    public val renderMarkdownInternally: Boolean = false,
) {
    /**
     * Resolves whether the active theme should be dark given the host platform's current dark state.
     *
     * @param systemInDarkTheme True if the operating system reports active dark mode.
     * @return True if dark theme should be rendered.
     */
    public fun isDark(systemInDarkTheme: Boolean): Boolean = when (uiMode) {
        UiMode.SYSTEM -> systemInDarkTheme
        UiMode.LIGHT -> false
        UiMode.DARK -> true
    }

    /**
     * Convenience flag indicating if [uiMode] is explicitly set to [UiMode.DARK].
     */
    public val darkTheme: Boolean
        get() = uiMode == UiMode.DARK
}
