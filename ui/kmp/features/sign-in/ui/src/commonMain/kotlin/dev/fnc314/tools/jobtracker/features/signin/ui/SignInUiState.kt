package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.compose.runtime.Immutable

@Immutable
public sealed interface SignInUiState {
    public data object Unauthenticated : SignInUiState
    public data object SigningIn : SignInUiState
    public data class Authenticated(public val user: UserAccountUiModel) : SignInUiState
    public data class Error(public val message: String) : SignInUiState
}
