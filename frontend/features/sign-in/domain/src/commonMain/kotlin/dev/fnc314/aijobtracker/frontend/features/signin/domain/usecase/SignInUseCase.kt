package dev.fnc314.aijobtracker.frontend.features.signin.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.model.UserAccount

/**
 * Use case initiating platform-specific user sign-in.
 */
public fun interface SignInUseCase {

    /**
     * Executes interactive sign-in.
     *
     * @return [Result] enclosing the authenticated [UserAccount] on success.
     */
    public suspend operator fun invoke(): Result<UserAccount>
}
