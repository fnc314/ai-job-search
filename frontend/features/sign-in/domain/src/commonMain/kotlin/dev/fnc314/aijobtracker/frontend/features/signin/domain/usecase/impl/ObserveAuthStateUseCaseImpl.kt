package dev.fnc314.aijobtracker.frontend.features.signin.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.AuthState
import dev.fnc314.aijobtracker.frontend.features.signin.data.repository.AuthRepository
import dev.fnc314.aijobtracker.frontend.features.signin.domain.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.flow.StateFlow

internal class ObserveAuthStateUseCaseImpl(
    private val authRepository: AuthRepository,
) : ObserveAuthStateUseCase {
    override fun invoke(): StateFlow<AuthState> = authRepository.authState
}
