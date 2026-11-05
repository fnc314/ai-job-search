package dev.fnc314.tools.jobtracker.features.trackeredits.data

import dev.fnc314.tools.jobtracker.core.data.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.*
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val trackerEditsDataModule = module {
    single<TrackerEditsRepository> { FirestoreTrackerEditsRepository() }
    factoryOf(::LoadPendingEditsUseCaseImpl) bind LoadPendingEditsUseCase::class
    factoryOf(::ApproveEditUseCaseImpl) bind ApproveEditUseCase::class
    factoryOf(::RejectEditUseCaseImpl) bind RejectEditUseCase::class
    factoryOf(::ExpireStaleEditsUseCaseImpl) bind ExpireStaleEditsUseCase::class
}
