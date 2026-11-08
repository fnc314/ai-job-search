package dev.fnc314.tools.jobtracker.features.signin.data.repository.impl

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.data.source.LocalAuthDataSource
import dev.fnc314.tools.jobtracker.features.signin.data.source.RemoteAuthDataSource
import dev.fnc314.tools.jobtracker.features.signin.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AuthRepositoryImpl(
    private val remoteDataSource: RemoteAuthDataSource,
    private val localDataSource: LocalAuthDataSource,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val currentUser: UserAccount?
        get() = (_authState.value as? AuthState.Authenticated)?.user

    override suspend fun signIn(): Result<UserAccount> {
        _authState.value = AuthState.Loading
        val result = remoteDataSource.signIn()
        return result.fold(
            onSuccess = { user ->
                _authState.value = AuthState.Authenticated(user)
                localDataSource.saveCachedUser(user)
                Result.success(user)
            },
            onFailure = { error ->
                _authState.value = AuthState.Error(error.message ?: "Authentication failed", error)
                Result.failure(error)
            }
        )
    }

    override suspend fun signOut(): Result<Unit> {
        val result = remoteDataSource.signOut()
        _authState.value = AuthState.Unauthenticated
        localDataSource.saveCachedUser(null)
        return result
    }

    override suspend fun initAuthState() {
        val cached = localDataSource.getCachedUser()
        if (cached != null) {
            _authState.value = AuthState.Authenticated(cached)
            return
        }
        val user = remoteDataSource.getCurrentUser()
        if (user != null) {
            _authState.value = AuthState.Authenticated(user)
            localDataSource.saveCachedUser(user)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }
}
