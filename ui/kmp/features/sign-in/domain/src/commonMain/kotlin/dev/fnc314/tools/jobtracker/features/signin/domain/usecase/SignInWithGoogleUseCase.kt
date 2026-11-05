package dev.fnc314.tools.jobtracker.features.signin.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

public interface SignInWithGoogleUseCase {
    public suspend operator fun invoke(): Result<UserAccount>
}
