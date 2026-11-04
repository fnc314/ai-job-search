package dev.fnc314.tools.jobtracker.features.signin.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.core.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveAuthStateUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<AuthState> = authRepository.authState
}

class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): UserAccount? = authRepository.currentUser
}

class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.signOut()
}
