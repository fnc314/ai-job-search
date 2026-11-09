package dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.GetCurrentUserUseCase

internal class GetCurrentUserUseCaseImpl(
    private val authRepository: AuthRepository,
) : GetCurrentUserUseCase {
    override fun invoke(): UserAccount? = authRepository.currentUser
}
