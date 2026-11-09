package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

public actual class PlatformSignInHandler {
    public actual suspend fun signIn(): Result<UserAccount> {
        return Result.failure(UnsupportedOperationException("Sign in not supported on JVM"))
    }

    public actual suspend fun getCurrentUser(): UserAccount? = null

    public actual suspend fun signOut(): Result<Unit> = Result.success(Unit)
}

public actual fun createPlatformSignInHandler(): PlatformSignInHandler = PlatformSignInHandler()
