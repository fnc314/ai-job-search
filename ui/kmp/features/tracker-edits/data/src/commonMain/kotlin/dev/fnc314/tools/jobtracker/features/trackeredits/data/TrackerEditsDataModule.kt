package dev.fnc314.tools.jobtracker.features.trackeredits.data

import dev.fnc314.tools.jobtracker.features.trackeredits.data.repository.TrackerEditsRepositoryImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.LocalTrackerEditsDataSource
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.LocalTrackerEditsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.RemoteTrackerEditsDataSource
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.RemoteTrackerEditsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ApproveEditUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ExpireStaleEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.LoadPendingEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.RejectEditUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val trackerEditsDataModule: Module = module {
    singleOf(::RemoteTrackerEditsDataSourceImpl) bind RemoteTrackerEditsDataSource::class
    singleOf(::LocalTrackerEditsDataSourceImpl) bind LocalTrackerEditsDataSource::class
    singleOf(::TrackerEditsRepositoryImpl) bind TrackerEditsRepository::class

    factoryOf(::LoadPendingEditsUseCaseImpl) bind LoadPendingEditsUseCase::class
    factoryOf(::ApproveEditUseCaseImpl) bind ApproveEditUseCase::class
    factoryOf(::RejectEditUseCaseImpl) bind RejectEditUseCase::class
    factoryOf(::ExpireStaleEditsUseCaseImpl) bind ExpireStaleEditsUseCase::class
}
