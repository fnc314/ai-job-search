package dev.fnc314.tools.jobtracker.features.signin.domain.usecase

/**
 * Use case signing out the active user session.
 */
public interface SignOutUseCase {

    /**
     * Signs out the user and invalidates credentials.
     *
     * @return [Result] indicating success or failure.
     */
    public suspend operator fun invoke(): Result<Unit>
}
