package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.compose.runtime.Immutable

@Immutable
public data class SettingsUiModel(
    public val themeModeName: String,
    public val seedColorHex: String,
    public val isSystemDark: Boolean,
)
