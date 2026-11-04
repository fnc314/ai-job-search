package dev.fnc314.tools.jobtracker.core.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentUser: UserAccount?
    suspend fun signOut(): Result<Unit>
}
