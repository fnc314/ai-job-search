package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

public expect class PlatformSignInHandler {
  public suspend fun signIn(): Result<UserAccount>

  public suspend fun getCurrentUser(): UserAccount?

  public suspend fun signOut(): Result<Unit>
}

public expect fun createPlatformSignInHandler(): PlatformSignInHandler
