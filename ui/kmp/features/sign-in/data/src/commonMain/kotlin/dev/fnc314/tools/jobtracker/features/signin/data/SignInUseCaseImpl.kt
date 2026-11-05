package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase

public class SignInUseCaseImpl(
    private val authRepository: DefaultAuthRepository
) : SignInUseCase {
    override suspend fun invoke(): Result<UserAccount> {
        return authRepository.signIn()
    }
}
