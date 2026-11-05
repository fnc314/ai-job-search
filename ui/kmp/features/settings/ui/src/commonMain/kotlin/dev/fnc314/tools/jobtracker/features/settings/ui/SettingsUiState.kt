package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.compose.runtime.Immutable

/**
 * Screen state model for the application settings screen.
 */
@Immutable
public sealed interface SettingsUiState {
    /** State indicating settings preferences are being loaded from persistent storage. */
    public data object Loading : SettingsUiState

    /**
     * Active state rendering configurable preferences.
     *
     * @property model Presentation values for themes and colors.
     */
    public data class Content(public val model: SettingsUiModel) : SettingsUiState

    /** Transient navigation state requesting return to the previous screen. */
    public data object NavigateBack : SettingsUiState
}
