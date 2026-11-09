package dev.fnc314.tools.jobtracker.features.signin.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource
import dev.fnc314.tools.jobtracker.features.signin.data.source.LocalAuthDataSource

internal class LocalAuthDataSourceImpl(
    private val localDataSource: LocalDataSource? = null,
) : LocalAuthDataSource {
    private var cachedUser: UserAccount? = null

    override suspend fun getCachedUser(): UserAccount? = cachedUser

    override suspend fun saveCachedUser(user: UserAccount?) {
        cachedUser = user
    }
}
