package dev.fnc314.tools.jobtracker.features.signin.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case observing the active [AuthState] stream.
 *
 * @param authRepository Backing [dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository] instance.
 */
public class ObserveAuthStateUseCase(
    private val authRepository: AuthRepository,
) {
    /**
     * Returns the stream emitting real-time authentication state updates.
     *
     * @return Hot [StateFlow] of [AuthState].
     */
    public operator fun invoke(): StateFlow<AuthState> = authRepository.authState
}

/**
 * Use case retrieving the currently signed-in user account.
 *
 * @param authRepository Backing [dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository] instance.
 */
public class GetCurrentUserUseCase(
    private val authRepository: AuthRepository,
) {
    /**
     * Resolves the current user identity.
     *
     * @return Active [UserAccount] or null if unauthenticated.
     */
    public operator fun invoke(): UserAccount? = authRepository.currentUser
}

/**
 * Use case signing out the active user session.
 *
 * @param authRepository Backing [dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository] instance.
 */
public class SignOutUseCase(
    private val authRepository: AuthRepository,
) {
    /**
     * Signs out the user and invalidates credentials.
     *
     * @return [Result] indicating success or failure.
     */
    public suspend operator fun invoke(): Result<Unit> = authRepository.signOut()
}
