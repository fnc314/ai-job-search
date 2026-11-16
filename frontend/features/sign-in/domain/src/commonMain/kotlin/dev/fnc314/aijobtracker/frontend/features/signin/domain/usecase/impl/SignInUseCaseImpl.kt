package dev.fnc314.aijobtracker.frontend.features.signin.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.UserAccount
import dev.fnc314.aijobtracker.frontend.features.signin.data.repository.AuthRepository
import dev.fnc314.aijobtracker.frontend.features.signin.domain.usecase.SignInUseCase

internal class SignInUseCaseImpl(
    private val authRepository: AuthRepository,
) : SignInUseCase {
    override suspend fun invoke(): Result<UserAccount> {
        return authRepository.signIn()
    }
}
