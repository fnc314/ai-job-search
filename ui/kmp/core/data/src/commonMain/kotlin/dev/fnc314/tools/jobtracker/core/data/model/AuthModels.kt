package dev.fnc314.tools.jobtracker.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAccount(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
)

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: UserAccount) : AuthState
    data class Error(val message: String, val cause: Throwable? = null) : AuthState
}
