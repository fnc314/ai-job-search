package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.jobApplicationsDetailsDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for the job application details feature.
 */
public val jobApplicationsDetailsUiModule: Module = module {
    viewModelOf(::JobApplicationDetailViewModel)
}

/**
 * Aggregate Koin module uniting data and UI definitions for the job application details feature.
 */
public val jobApplicationsDetailsFeatureModule: Module = module {
    includes(jobApplicationsDetailsDataModule, jobApplicationsDetailsUiModule)
}
