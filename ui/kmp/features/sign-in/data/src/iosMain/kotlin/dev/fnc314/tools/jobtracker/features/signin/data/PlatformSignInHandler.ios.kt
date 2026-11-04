package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

actual class PlatformSignInHandler {
    actual suspend fun signIn(): Result<UserAccount> {
        return Result.failure(UnsupportedOperationException("Sign in not supported on iOS directly yet"))
    }
    
    actual suspend fun getCurrentUser(): UserAccount? = null
    
    actual suspend fun signOut(): Result<Unit> = Result.success(Unit)
}
