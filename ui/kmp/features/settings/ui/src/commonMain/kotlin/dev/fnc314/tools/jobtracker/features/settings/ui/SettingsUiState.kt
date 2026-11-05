package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.compose.runtime.Immutable

@Immutable
public sealed interface SettingsUiState {
    public data object Loading : SettingsUiState
    public data class Content(public val model: SettingsUiModel) : SettingsUiState
    public data object NavigateBack : SettingsUiState
}
