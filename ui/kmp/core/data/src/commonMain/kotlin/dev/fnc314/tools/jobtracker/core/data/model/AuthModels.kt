package dev.fnc314.tools.jobtracker.core.data.model

import kotlinx.serialization.Serializable

@Serializable
public data class UserAccount(
    public val uid: String,
    public val email: String? = null,
    public val displayName: String? = null,
    public val photoUrl: String? = null,
)

public sealed interface AuthState {
    public data object Loading : AuthState
    public data object Unauthenticated : AuthState
    public data class Authenticated(public val user: UserAccount) : AuthState
    public data class Error(public val message: String, public val cause: Throwable? = null) : AuthState
}
