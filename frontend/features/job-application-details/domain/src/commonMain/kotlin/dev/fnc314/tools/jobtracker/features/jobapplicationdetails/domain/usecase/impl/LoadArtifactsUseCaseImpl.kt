package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadArtifactsUseCase

internal class LoadArtifactsUseCaseImpl(
    private val repository: JobApplicationDetailsRepository,
) : LoadArtifactsUseCase {
    override suspend fun invoke(params: LoadArtifactsUseCase.Params): List<TrackerArtifact> =
        repository.getArtifacts(params.applicationId)
}
