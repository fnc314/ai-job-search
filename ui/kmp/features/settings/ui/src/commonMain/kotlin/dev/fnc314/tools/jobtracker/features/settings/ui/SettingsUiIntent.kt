package dev.fnc314.tools.jobtracker.features.settings.ui

public sealed interface SettingsUiIntent {
    public data class ChangeTheme(public val isDark: Boolean) : SettingsUiIntent
    public data class ChangeSeedColor(public val hex: String) : SettingsUiIntent
    public data object ClickBack : SettingsUiIntent
}
