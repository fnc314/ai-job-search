package dev.fnc314.tools.jobtracker.features.signin.data.source

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.data.PlatformSignInHandler

public interface RemoteAuthDataSource {
    public suspend fun signIn(): Result<UserAccount>
    public suspend fun getCurrentUser(): UserAccount?
    public suspend fun signOut(): Result<Unit>
}

internal class RemoteAuthDataSourceImpl(
    private val signInHandler: PlatformSignInHandler,
) : RemoteAuthDataSource {
    override suspend fun signIn(): Result<UserAccount> = signInHandler.signIn()
    override suspend fun getCurrentUser(): UserAccount? = signInHandler.getCurrentUser()
    override suspend fun signOut(): Result<Unit> = signInHandler.signOut()
}
