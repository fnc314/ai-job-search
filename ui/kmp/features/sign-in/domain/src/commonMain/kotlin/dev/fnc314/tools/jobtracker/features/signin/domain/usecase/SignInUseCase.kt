package dev.fnc314.tools.jobtracker.features.signin.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

/**
 * Use case initiating platform-specific user sign-in.
 */
public interface SignInUseCase {

    /**
     * Executes interactive sign-in.
     *
     * @return [Result] enclosing the authenticated [UserAccount] on success.
     */
    public suspend operator fun invoke(): Result<UserAccount>
}
