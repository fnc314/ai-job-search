package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.jobApplicationsListDataModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val jobApplicationsListUiModule = module {
    viewModelOf(::JobApplicationsListViewModel)
}

val jobApplicationsListFeatureModule = module {
    includes(jobApplicationsListDataModule, jobApplicationsListUiModule)
}
