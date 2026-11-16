package dev.fnc314.aijobtracker.frontend.features.signin.domain.usecase

/**
 * Use case signing out the active user session.
 */
public fun interface SignOutUseCase {

    /**
     * Signs out the user and invalidates credentials.
     *
     * @return [Result] indicating success or failure.
     */
    public suspend operator fun invoke(): Result<Unit>
}
