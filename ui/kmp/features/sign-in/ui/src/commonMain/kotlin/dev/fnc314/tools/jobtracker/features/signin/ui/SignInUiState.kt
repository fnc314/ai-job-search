package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.compose.runtime.Immutable

/**
 * Screen state model for the user authentication screen.
 */
@Immutable
public sealed interface SignInUiState {
    /** State indicating no active user session exists. */
    public data object Unauthenticated : SignInUiState

    /** State indicating an authentication flow is actively negotiating credentials. */
    public data object SigningIn : SignInUiState

    /**
     * State indicating a user session is actively authenticated.
     *
     * @property user Presentation profile information for the authenticated user.
     */
    public data class Authenticated(public val user: UserAccountUiModel) : SignInUiState

    /**
     * State indicating authentication failed.
     *
     * @property message Descriptive failure explanation.
     */
    public data class Error(public val message: String) : SignInUiState
}
