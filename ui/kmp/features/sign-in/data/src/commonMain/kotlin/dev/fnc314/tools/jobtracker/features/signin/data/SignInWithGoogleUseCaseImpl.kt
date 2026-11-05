package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInWithGoogleUseCase

public class SignInWithGoogleUseCaseImpl(
    private val authRepository: DefaultAuthRepository
) : SignInWithGoogleUseCase {
    override suspend fun invoke(): Result<UserAccount> {
        return authRepository.signInWithGoogle()
    }
}
