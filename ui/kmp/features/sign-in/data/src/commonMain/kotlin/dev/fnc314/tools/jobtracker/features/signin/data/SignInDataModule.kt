package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepositoryImpl
import dev.fnc314.tools.jobtracker.features.signin.data.source.LocalAuthDataSource
import dev.fnc314.tools.jobtracker.features.signin.data.source.LocalAuthDataSourceImpl
import dev.fnc314.tools.jobtracker.features.signin.data.source.RemoteAuthDataSource
import dev.fnc314.tools.jobtracker.features.signin.data.source.RemoteAuthDataSourceImpl
import dev.fnc314.tools.jobtracker.features.signin.domain.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.GetCurrentUserUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.ObserveAuthStateUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val signInDataModule: Module = module {
    single { createPlatformSignInHandler() }
    singleOf(::RemoteAuthDataSourceImpl) bind RemoteAuthDataSource::class
    singleOf(::LocalAuthDataSourceImpl) bind LocalAuthDataSource::class
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class

    factoryOf(::SignInUseCaseImpl) bind SignInUseCase::class
    factoryOf(::ObserveAuthStateUseCase)
    factoryOf(::GetCurrentUserUseCase)
    factoryOf(::SignOutUseCase)
}
