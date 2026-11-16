package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.artifacts.model.TrackerArtifact
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.LoadArtifactsUseCase

internal class LoadArtifactsUseCaseImpl(
    private val repository: JobApplicationDetailsRepository,
) : LoadArtifactsUseCase {
    override suspend fun invoke(params: LoadArtifactsUseCase.Params): List<TrackerArtifact> =
        repository.getArtifacts(params.applicationId)
}
