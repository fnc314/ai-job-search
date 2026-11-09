package dev.fnc314.tools.jobtracker.core.data.model

import kotlinx.serialization.Serializable

/**
 * Represents an authenticated user identity and associated profile metadata.
 *
 * @property uid Unique Firebase user identity identifier.
 * @property email Primary email address of the account.
 * @property displayName User's public display name.
 * @property photoUrl Avatar image URL.
 */
@Serializable
public data class UserAccount(
    public val uid: String,
    public val email: String? = null,
    public val displayName: String? = null,
    public val photoUrl: String? = null,
)

/**
 * Reactive state machine governing Firebase authentication status.
 */
public sealed interface AuthState {
    /** Initial state during session startup or while checking cached tokens. */
    public data object Loading : AuthState

    /** State indicating no active authenticated user session exists. */
    public data object Unauthenticated : AuthState

    /**
     * Active authenticated session.
     *
     * @property user The authenticated user profile details [UserAccount].
     */
    public data class Authenticated(public val user: UserAccount) : AuthState

    /**
     * Terminal failure state encountered during sign-in or session renewal.
     *
     * @property message Descriptive error message suitable for user display.
     * @property cause The underlying exception that triggered the failure, if available.
     */
    public data class Error(public val message: String, public val cause: Throwable? = null) : AuthState
}
