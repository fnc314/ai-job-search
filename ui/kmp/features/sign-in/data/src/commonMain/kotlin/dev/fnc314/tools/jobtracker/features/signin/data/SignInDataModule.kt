package dev.fnc314.tools.jobtracker.features.signin.data

import dev.fnc314.tools.jobtracker.core.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.*
import org.koin.dsl.module

val signInDataModule = module {
    single { createPlatformSignInHandler() }
    single { DefaultAuthRepository(signInHandler = get()) }
    single<AuthRepository> { get<DefaultAuthRepository>() }
    factory<SignInWithGoogleUseCase> { SignInWithGoogleUseCaseImpl(get()) }
    factory { ObserveAuthStateUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { SignOutUseCase(get()) }
}
