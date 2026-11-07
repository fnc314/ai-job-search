package dev.fnc314.tools.jobtracker.features.settings.ui

import dev.fnc314.tools.jobtracker.core.data.settings.UiMode

/**
 * User-initiated intents dispatched to [SettingsViewModel].
 */
public sealed interface SettingsUiIntent {
    /** Changes the active theme mode ([UiMode]). */
    public data class ChangeUiMode(public val uiMode: UiMode) : SettingsUiIntent

    /** Sets the primary Material 3 seed palette color hex code. */
    public data class ChangeSeedColor(public val hex: String) : SettingsUiIntent

    /** Requests user sign out. */
    public data object ClickSignOut : SettingsUiIntent

    /** Requests back navigation to the preceding screen. */
    public data object ClickBack : SettingsUiIntent
}
