package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.di

import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.repository.impl.JobApplicationDetailsRepositoryImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.LocalJobApplicationDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.NetworkJobApplicationDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.RemoteJobApplicationDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.impl.LocalJobApplicationDetailsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.impl.NetworkJobApplicationDetailsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.impl.RemoteJobApplicationDetailsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring data layer dependencies for the job application details feature.
 */
public val jobApplicationDetailsDataModule: Module = module {
    singleOf(::RemoteJobApplicationDetailsDataSourceImpl) bind RemoteJobApplicationDetailsDataSource::class
    singleOf(::NetworkJobApplicationDetailsDataSourceImpl) bind NetworkJobApplicationDetailsDataSource::class
    singleOf(::LocalJobApplicationDetailsDataSourceImpl) bind LocalJobApplicationDetailsDataSource::class
    singleOf(::JobApplicationDetailsRepositoryImpl) bind JobApplicationDetailsRepository::class
}
