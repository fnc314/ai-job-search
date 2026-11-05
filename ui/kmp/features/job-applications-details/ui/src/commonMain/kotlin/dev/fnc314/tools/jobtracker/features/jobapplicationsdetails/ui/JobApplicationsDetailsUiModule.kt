package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.jobApplicationsDetailsDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val jobApplicationsDetailsUiModule: Module = module {
    viewModelOf(::JobApplicationDetailViewModel)
}

public val jobApplicationsDetailsFeatureModule: Module = module {
    includes(jobApplicationsDetailsDataModule, jobApplicationsDetailsUiModule)
}
