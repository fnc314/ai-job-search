package dev.fnc314.tools.jobtracker.features.settings.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean architecture use case contract observing the active [AppSettings] state stream.
 */
public fun interface ObserveSettingsUseCase {
    /**
     * Returns the observable [StateFlow] emitting current user preferences.
     *
     * @return Stream of [AppSettings].
     */
    public operator fun invoke(): StateFlow<AppSettings>
}
