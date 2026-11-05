package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.compose.runtime.Immutable

/**
 * Presentation model capturing user-configurable application preferences.
 *
 * @property themeModeName Serialized name of the active theme mode ([dev.fnc314.tools.jobtracker.core.data.settings.UiMode]).
 * @property seedColorHex Primary brand seed color formatted as `#RRGGBB` or `#AARRGGBB`.
 * @property isSystemDark Whether dark theme appearance is currently active.
 */
@Immutable
public data class SettingsUiModel(
    public val themeModeName: String,
    public val seedColorHex: String,
    public val isSystemDark: Boolean,
)
