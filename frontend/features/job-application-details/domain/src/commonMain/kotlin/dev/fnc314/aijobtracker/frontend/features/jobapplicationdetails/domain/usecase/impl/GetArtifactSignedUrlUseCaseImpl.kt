package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.GetArtifactSignedUrlUseCase

internal class GetArtifactSignedUrlUseCaseImpl(
  private val repository: JobApplicationDetailsRepository,
) : GetArtifactSignedUrlUseCase {
    override suspend fun invoke(params: GetArtifactSignedUrlUseCase.Params): String =
        repository.getArtifactSignedUrl(params.path)
}
