package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.settings.UiMode
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.ObserveSettingsUseCase
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Presentation ViewModel managing user appearance preferences and theme customization.
 *
 * @param observeSettingsUseCase Use case streaming persisted application configuration.
 * @param updateSettingsUseCase Use case persisting modified user settings.
 */
public class SettingsViewModel(
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
) : ViewModel() {

    private val _isNavigatingBack: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Observable stream of [SettingsUiState] emitting the current UI theme, color choices, and navigation triggers.
     */
    public val uiState: StateFlow<SettingsUiState> = combine(
        observeSettingsUseCase(),
        _isNavigatingBack,
    ) { settings: AppSettings, isNavigatingBack: Boolean ->
        if (isNavigatingBack) {
            SettingsUiState.NavigateBack
        } else {
            SettingsUiState.Content(
                model = SettingsUiModel(
                    themeModeName = settings.uiMode.name,
                    seedColorHex = settings.seedColorHex,
                    isSystemDark = settings.uiMode == UiMode.DARK,
                ),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState.Loading,
    )

    /**
     * Dispatches a user intent to be processed by this ViewModel.
     *
     * @param intent The [SettingsUiIntent] to process.
     */
    public fun onIntent(intent: SettingsUiIntent): Unit {
        when (intent) {
            is SettingsUiIntent.ChangeTheme -> handleChangeTheme(intent.isDark)
            is SettingsUiIntent.ChangeSeedColor -> handleChangeSeedColor(intent.hex)
            is SettingsUiIntent.ClickBack -> handleClickBack()
        }
    }

    private fun handleChangeTheme(isDark: Boolean): Unit {
        val currentSettings: AppSettings = observeSettingsUseCase().value
        val newMode: UiMode = if (isDark) UiMode.DARK else UiMode.LIGHT
        updateSettingsUseCase(currentSettings.copy(uiMode = newMode))
    }

    private fun handleChangeSeedColor(hex: String): Unit {
        val currentSettings: AppSettings = observeSettingsUseCase().value
        updateSettingsUseCase(currentSettings.copy(seedColorHex = hex))
    }

    private fun handleClickBack(): Unit {
        _isNavigatingBack.value = true
    }
}
