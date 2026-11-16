package dev.fnc314.aijobtracker.frontend.features.signin.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.model.UserAccount

/**
 * Use case retrieving the currently signed-in user account.
 */
public interface GetCurrentUserUseCase {

    /**
     * Resolves the current user identity.
     *
     * @return Active [UserAccount] or null if unauthenticated.
     */
    public operator fun invoke(): UserAccount?
}
