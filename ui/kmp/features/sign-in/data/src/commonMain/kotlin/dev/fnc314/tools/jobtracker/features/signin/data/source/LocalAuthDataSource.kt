package dev.fnc314.tools.jobtracker.features.signin.data.source

import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource

public interface LocalAuthDataSource {
    public suspend fun getCachedUser(): UserAccount?
    public suspend fun saveCachedUser(user: UserAccount?): Unit
}

internal class LocalAuthDataSourceImpl(
    private val localDataSource: LocalDataSource? = null,
) : LocalAuthDataSource {
    private var cachedUser: UserAccount? = null

    override suspend fun getCachedUser(): UserAccount? = cachedUser

    override suspend fun saveCachedUser(user: UserAccount?): Unit {
        cachedUser = user
    }
}
