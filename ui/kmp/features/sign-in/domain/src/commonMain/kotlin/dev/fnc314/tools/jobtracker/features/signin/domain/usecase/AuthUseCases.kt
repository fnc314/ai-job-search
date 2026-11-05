package dev.fnc314.tools.jobtracker.features.signin.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.core.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

public class ObserveAuthStateUseCase(
    private val authRepository: AuthRepository
) {
    public operator fun invoke(): StateFlow<AuthState> = authRepository.authState
}

public class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    public operator fun invoke(): UserAccount? = authRepository.currentUser
}

public class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    public suspend operator fun invoke(): Result<Unit> = authRepository.signOut()
}
