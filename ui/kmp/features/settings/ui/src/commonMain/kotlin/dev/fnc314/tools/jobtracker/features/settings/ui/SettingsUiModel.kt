package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.settings.UiMode

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
