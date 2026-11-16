package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.di

import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.data.di.jobApplicationsListDataModule
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.domain.di.jobApplicationsListDomainModule
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.JobApplicationsEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.JobApplicationsListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val jobApplicationsListUiModule: Module = module {
    viewModelOf(::JobApplicationsListViewModel)
    singleOf(::JobApplicationsEntryRegistry)
}

/**
 * Composite Koin feature module bundling job applications list data, domain, and UI dependencies.
 */
public val jobApplicationsListFeatureModule: Module = module {
    includes(jobApplicationsListDataModule, jobApplicationsListDomainModule, jobApplicationsListUiModule)
}
