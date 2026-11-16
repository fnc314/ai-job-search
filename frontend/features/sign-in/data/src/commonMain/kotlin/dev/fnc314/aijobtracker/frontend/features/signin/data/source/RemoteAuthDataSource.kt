package dev.fnc314.aijobtracker.frontend.features.signin.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.UserAccount

internal interface RemoteAuthDataSource {
  suspend fun signIn(): Result<UserAccount>

  suspend fun getCurrentUser(): UserAccount?

  suspend fun signOut(): Result<Unit>
}
