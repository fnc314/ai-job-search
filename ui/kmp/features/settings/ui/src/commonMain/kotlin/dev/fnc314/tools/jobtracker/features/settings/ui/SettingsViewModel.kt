package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.lifecycle.ViewModel
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.ObserveSettingsUseCase
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.StateFlow

public class SettingsViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
) : ViewModel() {
    public val settings: StateFlow<AppSettings> = observeSettingsUseCase()

    public fun updateSettings(newSettings: AppSettings) {
        updateSettingsUseCase(newSettings)
    }
}
