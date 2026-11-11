package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.di

import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.di.jobApplicationsListDataModule
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.di.jobApplicationsListDomainModule
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationsEntryRegistry
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationsListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.single

public val jobApplicationsListUiModule: Module = module {
    viewModelOf(::JobApplicationsListViewModel)
    single<FeatureEntryRegistry> { JobApplicationsEntryRegistry() }
}

public val jobApplicationsListFeatureModule: Module = module {
    includes(jobApplicationsListDataModule, jobApplicationsListDomainModule, jobApplicationsListUiModule)
}
