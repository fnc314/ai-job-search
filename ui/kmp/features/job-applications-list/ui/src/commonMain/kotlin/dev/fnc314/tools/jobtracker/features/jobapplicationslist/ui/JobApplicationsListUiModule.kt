package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.jobApplicationsListDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val jobApplicationsListUiModule: Module = module {
    viewModelOf(::JobApplicationsListViewModel)
}

public val jobApplicationsListFeatureModule: Module = module {
    includes(jobApplicationsListDataModule, jobApplicationsListUiModule)
}
