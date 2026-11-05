package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileOpener
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.core.data.repository.KtorStorageRepository
import dev.fnc314.tools.jobtracker.core.data.repository.StorageRepository
import dev.fnc314.tools.jobtracker.core.data.settings.SettingsStorage
import dev.fnc314.tools.jobtracker.core.data.settings.createSettingsStorage
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.addJobApplicationFeatureModule
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.jobApplicationsDetailsFeatureModule
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.jobApplicationsListFeatureModule
import dev.fnc314.tools.jobtracker.features.settings.ui.settingsFeatureModule
import dev.fnc314.tools.jobtracker.features.signin.ui.signInFeatureModule
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.trackerEditsFeatureModule
import dev.fnc314.tools.jobtracker.platform.PlatformAuth
import io.ktor.client.*
import org.koin.core.module.Module
import org.koin.dsl.module

public val repositoryModule: Module = module {
    single<JobApplicationRepository> { createJobApplicationRepository() }
    single<StorageRepository> {
        KtorStorageRepository(get())
    }
    single<SettingsStorage> { createSettingsStorage() }
}

public val platformModule: Module = module {
    single { PlatformAuth() }
    single { PlatformFileOpener() }
    single { HttpClient() }
}

public val appModule: Module = module {
    includes(
        repositoryModule,
        platformModule,
        signInFeatureModule,
        settingsFeatureModule,
        addJobApplicationFeatureModule,
        trackerEditsFeatureModule,
        jobApplicationsListFeatureModule,
        jobApplicationsDetailsFeatureModule,
    )
}
