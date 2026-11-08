package dev.fnc314.tools.jobtracker.features.signin.data.source

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount

internal interface LocalAuthDataSource {
    suspend fun getCachedUser(): UserAccount?
    suspend fun saveCachedUser(user: UserAccount?)
}
