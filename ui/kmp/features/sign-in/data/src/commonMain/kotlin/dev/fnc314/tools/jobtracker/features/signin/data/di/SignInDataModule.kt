package dev.fnc314.tools.jobtracker.features.signin.data.di

import dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.data.repository.impl.AuthRepositoryImpl
import dev.fnc314.tools.jobtracker.features.signin.data.source.LocalAuthDataSource
import dev.fnc314.tools.jobtracker.features.signin.data.source.RemoteAuthDataSource
import dev.fnc314.tools.jobtracker.features.signin.data.source.impl.LocalAuthDataSourceImpl
import dev.fnc314.tools.jobtracker.features.signin.data.source.impl.RemoteAuthDataSourceImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring data layer dependencies for the sign-in feature.
 */
public val signInDataModule: Module = module {
    singleOf(::RemoteAuthDataSourceImpl) bind RemoteAuthDataSource::class
    singleOf(::LocalAuthDataSourceImpl) bind LocalAuthDataSource::class
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class

}