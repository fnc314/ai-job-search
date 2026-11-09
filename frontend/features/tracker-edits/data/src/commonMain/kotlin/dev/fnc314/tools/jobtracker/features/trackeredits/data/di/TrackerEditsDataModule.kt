package dev.fnc314.tools.jobtracker.features.trackeredits.data.di

import dev.fnc314.tools.jobtracker.features.trackeredits.data.repository.impl.TrackerEditsRepositoryImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.LocalTrackerEditsDataSource
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.RemoteTrackerEditsDataSource
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.impl.LocalTrackerEditsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.impl.RemoteTrackerEditsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository.TrackerEditsRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring data layer dependencies for the tracker edits feature.
 */
public val trackerEditsDataModule: Module = module {
    singleOf(::RemoteTrackerEditsDataSourceImpl) bind RemoteTrackerEditsDataSource::class
    singleOf(::LocalTrackerEditsDataSourceImpl) bind LocalTrackerEditsDataSource::class
    singleOf(::TrackerEditsRepositoryImpl) bind TrackerEditsRepository::class
}
