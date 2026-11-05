package dev.fnc314.tools.jobtracker.features.settings.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.features.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean architecture use case observing the active [AppSettings] state stream.
 *
 * @param repository The backing [SettingsRepository].
 */
public class ObserveSettingsUseCase(
    private val repository: SettingsRepository,
) {
    /**
     * Returns the observable [StateFlow] emitting current user preferences.
     *
     * @return Stream of [AppSettings].
     */
    public operator fun invoke(): StateFlow<AppSettings> = repository.settings
}
