package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.core.data.di.coreDataModule
import dev.fnc314.tools.jobtracker.core.network.di.coreNetworkModule
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.addJobApplicationFeatureModule
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.jobApplicationsDetailsFeatureModule
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.di.jobApplicationsListFeatureModule
import dev.fnc314.tools.jobtracker.features.settings.ui.settingsFeatureModule
import dev.fnc314.tools.jobtracker.features.signin.ui.signInFeatureModule
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.trackerEditsFeatureModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Master Koin module assembling all core, data, network, and feature modules for the application runtime.
 */
public val appModule: Module = module {
    includes(
        coreDataModule,
        coreNetworkModule,
        signInFeatureModule,
        settingsFeatureModule,
        addJobApplicationFeatureModule,
        trackerEditsFeatureModule,
        jobApplicationsListFeatureModule,
        jobApplicationsDetailsFeatureModule,
    )
}
