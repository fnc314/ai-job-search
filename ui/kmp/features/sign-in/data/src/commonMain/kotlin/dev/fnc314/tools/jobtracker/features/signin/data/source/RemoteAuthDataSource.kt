package dev.fnc314.tools.jobtracker.features.signin.data.source

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

internal interface RemoteAuthDataSource {
    suspend fun signIn(): Result<UserAccount>
    suspend fun getCurrentUser(): UserAccount?
    suspend fun signOut(): Result<Unit>
}
