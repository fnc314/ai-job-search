package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.di

import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.di.jobApplicationDetailsDataModule
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.di.jobApplicationsDetailsDomainModule
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.JobApplicationDetailViewModel
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.JobApplicationDetailsEntryRegistry
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.single

/**
 * Koin module declaring UI layer dependencies for the job application details feature.
 */
public val jobApplicationDetailsUiModule: Module = module {
    viewModelOf(::JobApplicationDetailViewModel)
    single<FeatureEntryRegistry> { JobApplicationDetailsEntryRegistry() }
}

/**
 * Aggregate Koin module uniting data, domain, and UI definitions for the job application details feature.
 */
public val jobApplicationDetailsFeatureModule: Module = module {
    includes(jobApplicationDetailsDataModule, jobApplicationsDetailsDomainModule, jobApplicationDetailsUiModule)
}
