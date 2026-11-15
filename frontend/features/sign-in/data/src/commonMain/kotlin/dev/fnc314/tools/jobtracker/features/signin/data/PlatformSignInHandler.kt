package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

internal expect class PlatformSignInHandler {
  suspend fun signIn(): Result<UserAccount>

  suspend fun getCurrentUser(): UserAccount?

  suspend fun signOut(): Result<Unit>
}

internal expect fun createPlatformSignInHandler(): PlatformSignInHandler
