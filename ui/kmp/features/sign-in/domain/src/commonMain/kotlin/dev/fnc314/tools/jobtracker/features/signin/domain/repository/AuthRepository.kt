package dev.fnc314.tools.jobtracker.features.signin.domain.repository

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import kotlinx.coroutines.flow.StateFlow

public interface AuthRepository {
    public val authState: StateFlow<AuthState>
    public val currentUser: UserAccount?
    public suspend fun signIn(): Result<UserAccount>
    public suspend fun signOut(): Result<Unit>
    public suspend fun initAuthState(): Unit
}
