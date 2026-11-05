package dev.fnc314.tools.jobtracker.features.signin.ui

import dev.fnc314.tools.jobtracker.features.signin.data.signInDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

public val signInUiModule: Module = module {
    factoryOf(::SignInViewModel)
}

public val signInFeatureModule: Module = module {
    includes(signInDataModule, signInUiModule)
}
