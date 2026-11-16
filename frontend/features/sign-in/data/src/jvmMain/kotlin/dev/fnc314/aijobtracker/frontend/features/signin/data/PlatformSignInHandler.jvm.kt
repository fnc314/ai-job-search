package dev.fnc314.aijobtracker.frontend.features.signin.data

import dev.fnc314.aijobtracker.frontend.core.data.model.UserAccount

internal actual class PlatformSignInHandler {
    actual suspend fun signIn(): Result<UserAccount> {
        return Result.failure(UnsupportedOperationException("Sign in not supported on JVM"))
    }

    actual suspend fun getCurrentUser(): UserAccount? = null

    actual suspend fun signOut(): Result<Unit> = Result.success(Unit)
}

internal actual fun createPlatformSignInHandler(): PlatformSignInHandler = PlatformSignInHandler()
