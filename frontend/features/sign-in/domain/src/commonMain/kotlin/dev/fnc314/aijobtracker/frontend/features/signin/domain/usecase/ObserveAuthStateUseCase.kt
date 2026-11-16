package dev.fnc314.aijobtracker.frontend.features.signin.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.model.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case observing the active [AuthState] stream.
 */
public fun interface ObserveAuthStateUseCase {

    /**
     * Returns the stream emitting real-time authentication state updates.
     *
     * @return Hot [StateFlow] of [AuthState].
     */
    public operator fun invoke(): StateFlow<AuthState>
}
