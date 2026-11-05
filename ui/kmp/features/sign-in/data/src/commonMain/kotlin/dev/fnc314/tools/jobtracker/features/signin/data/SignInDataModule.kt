package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.core.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.GetCurrentUserUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.ObserveAuthStateUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase
import org.koin.dsl.module

val signInDataModule = module {
    single { createPlatformSignInHandler() }
    single { DefaultAuthRepository(signInHandler = get()) }
    single<AuthRepository> { get<DefaultAuthRepository>() }
    factory<SignInUseCase> { SignInUseCaseImpl(get()) }
    factory { ObserveAuthStateUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { SignOutUseCase(get()) }
}
