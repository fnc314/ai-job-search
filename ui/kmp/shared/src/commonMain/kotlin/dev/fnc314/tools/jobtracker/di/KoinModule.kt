package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.data.firestore.FirestoreJobApplicationRepository
import dev.fnc314.tools.jobtracker.data.firestore.FirestoreTrackerEditsRepository
import dev.fnc314.tools.jobtracker.data.gcs.KtorStorageRepository
import dev.fnc314.tools.jobtracker.domain.impl.usecase.*
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.domain.repository.StorageRepository
import dev.fnc314.tools.jobtracker.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.domain.usecase.*
import dev.fnc314.tools.jobtracker.ui.add.AddJobApplicationViewModel
import dev.fnc314.tools.jobtracker.ui.detail.JobApplicationDetailViewModel
import dev.fnc314.tools.jobtracker.ui.lead.AddLeadViewModel
import dev.fnc314.tools.jobtracker.ui.list.JobApplicationsListViewModel
import dev.fnc314.tools.jobtracker.platform.PlatformAuth
import dev.fnc314.tools.jobtracker.platform.PlatformFileOpener
import io.ktor.client.*
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    single<JobApplicationRepository> { FirestoreJobApplicationRepository() }
    single<TrackerEditsRepository> { FirestoreTrackerEditsRepository() }
    single<StorageRepository> {
        KtorStorageRepository(get())
    }
}

val useCaseModule = module {
    factoryOf(::LoadAllJobApplicationsUseCaseImpl) bind LoadAllJobApplicationsUseCase::class
    factoryOf(::LoadJobApplicationUseCaseImpl) bind LoadJobApplicationUseCase::class
    factoryOf(::LoadCompaniesUseCaseImpl) bind LoadCompaniesUseCase::class
    factory { FilterJobApplicationsUseCaseImpl() } bind FilterJobApplicationsUseCase::class
    factory { SortJobApplicationsUseCaseImpl() } bind SortJobApplicationsUseCase::class
    factoryOf(::UpdateApplicationStatusUseCaseImpl) bind UpdateApplicationStatusUseCase::class
    factoryOf(::RecordEventUseCaseImpl) bind RecordEventUseCase::class
    factoryOf(::UpdateFitScoreUseCaseImpl) bind UpdateFitScoreUseCase::class
    factoryOf(::GetArtifactSignedUrlUseCaseImpl) bind GetArtifactSignedUrlUseCase::class
    factoryOf(::CreateJobApplicationUseCaseImpl) bind CreateJobApplicationUseCase::class
    factoryOf(::CreateLeadUseCaseImpl) bind CreateLeadUseCase::class
    factoryOf(::LoadPendingEditsUseCaseImpl) bind LoadPendingEditsUseCase::class
    factoryOf(::ApproveEditUseCaseImpl) bind ApproveEditUseCase::class
    factoryOf(::RejectEditUseCaseImpl) bind RejectEditUseCase::class
    factoryOf(::ExpireStaleEditsUseCaseImpl) bind ExpireStaleEditsUseCase::class
}

val platformModule = module {
    single { PlatformAuth() }
    single { PlatformFileOpener() }
    single {
        HttpClient()
    }
}

val viewModelModule = module {
    factory { (
        JobApplicationsListViewModel(
            loadAll = get(),
            filterApplications = get(),
            sortApplications = get(),
        )
    ) }
    singleOf(::JobApplicationDetailViewModel)
    factoryOf(::AddJobApplicationViewModel)
    factoryOf(::AddLeadViewModel)
}

val appModule = module {
    includes(repositoryModule, useCaseModule, platformModule, viewModelModule)
}
