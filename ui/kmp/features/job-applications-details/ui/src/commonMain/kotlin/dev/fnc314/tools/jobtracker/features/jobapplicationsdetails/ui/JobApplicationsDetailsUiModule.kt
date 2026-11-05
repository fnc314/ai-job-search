package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.jobApplicationsDetailsDataModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val jobApplicationsDetailsUiModule = module {
    viewModelOf(::JobApplicationDetailViewModel)
}

val jobApplicationsDetailsFeatureModule = module {
    includes(jobApplicationsDetailsDataModule, jobApplicationsDetailsUiModule)
}
