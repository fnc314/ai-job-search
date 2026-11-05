package dev.fnc314.tools.jobtracker.core.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import kotlinx.coroutines.flow.StateFlow

public interface AuthRepository {
    public val authState: StateFlow<AuthState>
    public val currentUser: UserAccount?
    public suspend fun signOut(): Result<Unit>
    /**
     * Restores the authentication state from the platform's persistent auth store
     * (e.g., Firebase Auth on Android). Call this once during app initialization.
     */
    public suspend fun initAuthState(): Unit
}
