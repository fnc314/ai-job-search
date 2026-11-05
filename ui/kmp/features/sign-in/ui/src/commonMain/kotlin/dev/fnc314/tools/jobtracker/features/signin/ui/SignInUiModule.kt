package dev.fnc314.tools.jobtracker.features.signin.ui

import dev.fnc314.tools.jobtracker.features.signin.data.signInDataModule
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val signInUiModule = module {
    factoryOf(::SignInViewModel)
}

val signInFeatureModule = module {
    includes(signInDataModule, signInUiModule)
}
