package dev.fnc314.tools.jobtracker.features.signin.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case observing the active [AuthState] stream.
 */
public interface ObserveAuthStateUseCase {

    /**
     * Returns the stream emitting real-time authentication state updates.
     *
     * @return Hot [StateFlow] of [AuthState].
     */
    public operator fun invoke(): StateFlow<AuthState>
}
