package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.compose.runtime.Immutable
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

/**
 * Presentation model capturing user-configurable application preferences.
 *
 * @property uiMode Active theme mode ([UiMode]).
 * @property seedColorHex Primary brand seed color formatted as `#RRGGBB` or `#AARRGGBB`.
 * @property isSystemDark Whether dark theme appearance is currently active.
 * @property randomSeedSuggestions Curated preset seed color hex strings for quick selection.
 */
@Immutable
public data class SettingsUiModel(
    public val uiMode: UiMode,
    public val seedColorHex: String,
    public val isSystemDark: Boolean,
    public val randomSeedSuggestions: List<String>,
)