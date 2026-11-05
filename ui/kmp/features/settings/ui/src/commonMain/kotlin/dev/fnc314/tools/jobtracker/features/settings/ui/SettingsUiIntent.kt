package dev.fnc314.tools.jobtracker.features.settings.ui

/**
 * User-initiated intents dispatched to [SettingsViewModel].
 */
public sealed interface SettingsUiIntent {
    /** Toggles between light and dark theme mode. */
    public data class ChangeTheme(public val isDark: Boolean) : SettingsUiIntent

    /** Sets the primary Material 3 seed palette color hex code. */
    public data class ChangeSeedColor(public val hex: String) : SettingsUiIntent

    /** Requests back navigation to the preceding screen. */
    public data object ClickBack : SettingsUiIntent
}
