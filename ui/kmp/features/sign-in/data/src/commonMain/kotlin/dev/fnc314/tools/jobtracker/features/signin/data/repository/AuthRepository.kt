package dev.fnc314.tools.jobtracker.features.signin.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository contract managing user authentication lifecycle and credentials.
 */
public interface AuthRepository {

    /**
     * Observable stream emitting real-time authentication status [dev.fnc314.tools.jobtracker.core.data.model.AuthState].
     */
    public val authState: StateFlow<AuthState>

    /**
     * Currently authenticated user profile [dev.fnc314.tools.jobtracker.core.data.model.UserAccount], or null if unauthenticated.
     */
    public val currentUser: UserAccount?

    /**
     * Initiates the interactive sign-in flow.
     *
     * @return [Result] enclosing the authenticated [UserAccount] on success or an error on failure.
     */
    public suspend fun signIn(): Result<UserAccount>

    /**
     * Signs out the currently authenticated user and clears session tokens.
     *
     * @return [Result] enclosing completion unit on success or an error on failure.
     */
    public suspend fun signOut(): Result<Unit>

    /**
     * Initializes authentication state during application bootstrap.
     */
    public suspend fun initAuthState(): Unit
}
