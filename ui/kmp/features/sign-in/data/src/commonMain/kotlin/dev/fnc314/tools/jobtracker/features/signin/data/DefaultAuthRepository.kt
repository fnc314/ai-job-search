package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.core.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

expect class PlatformSignInHandler {
    suspend fun signIn(): Result<UserAccount>
    suspend fun getCurrentUser(): UserAccount?
    suspend fun signOut(): Result<Unit>
}

class DefaultAuthRepository(
    private val signInHandler: PlatformSignInHandler,
    initialUser: UserAccount? = null
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(
        if (initialUser != null) AuthState.Authenticated(initialUser) else AuthState.Unauthenticated
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val currentUser: UserAccount?
        get() = (_authState.value as? AuthState.Authenticated)?.user

    suspend fun signInWithGoogle(): Result<UserAccount> {
        println("[DefaultAuthRepository] signInWithGoogle: Setting state to Loading")
        _authState.value = AuthState.Loading
        println("[DefaultAuthRepository] signInWithGoogle: Calling platform sign-in handler")
        val result = signInHandler.signIn()
        return result.fold(
            onSuccess = { user ->
                println("[DefaultAuthRepository] signInWithGoogle: Success - user ${user.uid}")
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            },
            onFailure = { error ->
                println("[DefaultAuthRepository] signInWithGoogle: Failed - ${error.message}")
                _authState.value = AuthState.Error(error.message ?: "Authentication failed", error)
                Result.failure(error)
            }
        )
    }

    override suspend fun signOut(): Result<Unit> {
        println("[DefaultAuthRepository] signOut: Signing out")
        val result = signInHandler.signOut()
        _authState.value = AuthState.Unauthenticated
        println("[DefaultAuthRepository] signOut: Complete")
        return result
    }

    override suspend fun initAuthState() {
        println("[DefaultAuthRepository] initAuthState: Checking for existing session")
        val user = signInHandler.getCurrentUser()
        if (user != null) {
            println("[DefaultAuthRepository] initAuthState: Restored session for user ${user.uid}")
            _authState.value = AuthState.Authenticated(user)
        } else {
            println("[DefaultAuthRepository] initAuthState: No existing session")
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun updateAuthState(state: AuthState) {
        println("[DefaultAuthRepository] updateAuthState: $state")
        _authState.value = state
    }
}
