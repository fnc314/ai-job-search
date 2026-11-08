package dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.domain.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase

internal class SignInUseCaseImpl(
    private val authRepository: AuthRepository,
) : SignInUseCase {
    override suspend fun invoke(): Result<UserAccount> {
        return authRepository.signIn()
    }
}
